package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.lang.ref.WeakReference

/**
 * An Accessibility Service that allows the assistant to read and analyze active screen content
 * (for OCR / screen-reading / context understanding) and interact with UI elements programmatically.
 */
class AssistantAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "🟢 Assistant Accessibility Service connected!")
        serviceInstance = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Log of interest events to trace user activity or window switches
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Window state changed: Package=${event.packageName}, Class=${event.className}")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.v(TAG, "View clicked: ${event.text}")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "⚠️ Assistant Accessibility Service interrupted!")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔴 Assistant Accessibility Service destroyed!")
        if (serviceInstance.get() == this) {
            serviceInstance = WeakReference(null)
        }
    }

    /**
     * Data representation of a parsed UI node on the screen.
     */
    data class ScreenNode(
        val text: String?,
        val contentDescription: String?,
        val resourceId: String?,
        val bounds: Rect,
        val isClickable: Boolean,
        val isScrollable: Boolean,
        val isEditable: Boolean,
        val className: String
    ) {
        fun getDisplayLabel(): String {
            return text ?: contentDescription ?: ""
        }
    }

    /**
     * Recursively dumps the visible screen hierarchy into a list of simplified ScreenNodes.
     */
    fun dumpScreenNodes(): List<ScreenNode> {
        val nodes = mutableListOf<ScreenNode>()
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            parseNodeRecursive(rootNode, nodes)
        } else {
            Log.w(TAG, "rootInActiveWindow is null. Cannot capture screen content.")
        }
        return nodes
    }

    private fun parseNodeRecursive(node: AccessibilityNodeInfo?, list: MutableList<ScreenNode>) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val screenNode = ScreenNode(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            resourceId = node.viewIdResourceName,
            bounds = bounds,
            isClickable = node.isClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            className = node.className?.toString() ?: "Unknown"
        )

        // Only keep nodes that have either text, content description, resource ID, or are actionable
        if (!screenNode.text.isNullOrEmpty() || 
            !screenNode.contentDescription.isNullOrEmpty() || 
            !screenNode.resourceId.isNullOrEmpty() || 
            screenNode.isClickable || 
            screenNode.isScrollable || 
            screenNode.isEditable
        ) {
            list.add(screenNode)
        }

        for (i in 0 until node.childCount) {
            parseNodeRecursive(node.getChild(i), list)
        }
    }

    /**
     * Generates a simplified text summary of the current screen context,
     * perfect for sending as prompt context to the Gemini/NLU model.
     */
    fun getScreenTextSummary(): String {
        val nodes = dumpScreenNodes()
        val sb = StringBuilder()
        sb.append("Current Screen Content:\n")
        
        val visibleTextElements = nodes.filter { !it.getDisplayLabel().isEmpty() }
        if (visibleTextElements.isEmpty()) {
            sb.append("[No text or accessible elements visible on the screen]")
        } else {
            visibleTextElements.forEach { node ->
                val label = node.getDisplayLabel()
                val typeInfo = when {
                    node.isEditable -> "[Input Field]"
                    node.isClickable -> "[Button/Clickable]"
                    else -> "[Text]"
                }
                sb.append("- $typeInfo $label (Resource ID: ${node.resourceId ?: "None"})\n")
            }
        }
        return sb.toString()
    }

    /**
     * Searches for a UI node matching the target text and triggers a click action.
     */
    fun clickText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(targetText)
        if (nodes != null && nodes.isNotEmpty()) {
            for (node in nodes) {
                if (performClickOnNode(node)) {
                    Log.d(TAG, "Successfully clicked element with text: $targetText")
                    return true
                }
            }
        }
        Log.w(TAG, "Could not click element with text: $targetText")
        return false
    }

    /**
     * Searches for a UI node matching the resource ID and triggers a click action.
     */
    fun clickNodeByResourceId(resourceId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(resourceId)
        if (nodes != null && nodes.isNotEmpty()) {
            for (node in nodes) {
                if (performClickOnNode(node)) {
                    Log.d(TAG, "Successfully clicked element with ID: $resourceId")
                    return true
                }
            }
        }
        Log.w(TAG, "Could not click element with ID: $resourceId")
        return false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // If the element itself is not clickable, try its parent (common for composite views / layouts)
        val parent = node.parent
        return if (parent != null) {
            performClickOnNode(parent)
        } else {
            false
        }
    }

    /**
     * Programmatically performs a simulated tap at specific (x, y) coordinates.
     */
    fun clickAtCoordinates(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null) {
        val path = Path()
        path.moveTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        val strokeDescription = GestureDescription.StrokeDescription(path, 0, 100)
        gestureBuilder.addStroke(strokeDescription)
        
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d(TAG, "Simulated tap at ($x, $y) completed successfully.")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.e(TAG, "Simulated tap at ($x, $y) was cancelled.")
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Types text into a node with the given target text/ID or the currently focused node.
     */
    fun enterTextIntoActiveNode(textToInput: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && focusedNode.isEditable) {
            val arguments = android.os.Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                textToInput
            )
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (success) {
                Log.d(TAG, "Entered text '$textToInput' into focused input field.")
                return true
            }
        }
        Log.w(TAG, "Could not find a focused editable input field to enter text.")
        return false
    }

    /**
     * Programmatically scrolls forward or backward.
     */
    fun scroll(forward: Boolean): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        return performScrollRecursive(rootNode, forward)
    }

    private fun performScrollRecursive(node: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        if (node == null) return false
        if (node.isScrollable) {
            val action = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (node.performAction(action)) {
                Log.d(TAG, "Scrolled ${if (forward) "forward" else "backward"} on node.")
                return true
            }
        }
        for (i in 0 until node.childCount) {
            if (performScrollRecursive(node.getChild(i), forward)) {
                return true
            }
        }
        return false
    }

    // --- Global Actions Convenience Methods ---
    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun pullNotifications() = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    companion object {
        private const val TAG = "AssistantAccessibility"
        
        private var serviceInstance = WeakReference<AssistantAccessibilityService>(null)

        /**
         * Checks if the Accessibility Service is currently active and running.
         */
        val isServiceRunning: Boolean
            get() = serviceInstance.get() != null

        /**
         * Returns the running service instance, or null if it hasn't been enabled in Android Settings.
         */
        val instance: AssistantAccessibilityService?
            get() = serviceInstance.get()
    }
}
