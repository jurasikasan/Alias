package jurasikasan.alias

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.customview.widget.ViewDragHelper


open class SwipeDismissBehavior<V : View?> :
    CoordinatorLayout.Behavior<V>() {

    var viewDragHelper: ViewDragHelper? = null

    @get:VisibleForTesting
    var listener: OnDismissListener? = null
    private var interceptingEvents = false
    private var sensitivity = 0f
    private var sensitivitySet = false
    var dragDismissThreshold = DEFAULT_DRAG_DISMISS_THRESHOLD
    var alphaStartSwipeDistance = DEFAULT_ALPHA_START_DISTANCE
    var alphaEndSwipeDistance = DEFAULT_ALPHA_END_DISTANCE

    /** Callback interface used to notify the application that the view has been dismissed.  */
    interface OnDismissListener {
        /** Called when `view` has been dismissed via swiping.  */
        fun onDismissUp(view: View?)
        fun onDismissDown(view: View?)

        /**
         * Called when the drag state has changed.
         *
         * @param state the new state. One of [.STATE_IDLE], [.STATE_DRAGGING] or [     ][.STATE_SETTLING].
         */
        fun onDragStateChanged(state: Int)
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout, child: V, layoutDirection: Int
    ): Boolean {
        val handled = super.onLayoutChild(parent, child, layoutDirection)
        if (ViewCompat.getImportantForAccessibility(child!!)
            == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        ) {
            ViewCompat.setImportantForAccessibility(
                child,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES
            )
            updateAccessibilityActions(child)
        }
        return handled
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout, child: V, event: MotionEvent
    ): Boolean {
        var dispatchEventToHelper = interceptingEvents
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interceptingEvents = parent.isPointInChildBounds(
                    child!!, event.x.toInt(),
                    event.y.toInt()
                )
                dispatchEventToHelper = interceptingEvents
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->         // Reset the ignore flag for next times
                interceptingEvents = false
        }
        if (dispatchEventToHelper) {
            ensureViewDragHelper(parent)
            return viewDragHelper!!.shouldInterceptTouchEvent(event)
        }
        return false
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if (viewDragHelper != null) {
            viewDragHelper!!.processTouchEvent(event)
            return true
        }
        return false
    }

    /**
     * Called when the user's input indicates that they want to swipe the given view.
     *
     * @param view View the user is attempting to swipe
     * @return true if the view can be dismissed via swiping, false otherwise
     */
    open fun canSwipeDismissView(view: View): Boolean {
        return true
    }

    private val dragCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        private val INVALID_POINTER_ID = -1
        private var originalCapturedViewTop = 0
        private var activePointerId = INVALID_POINTER_ID
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            // Only capture if we don't already have an active pointer id
            return ((activePointerId == INVALID_POINTER_ID || activePointerId == pointerId)
                    && canSwipeDismissView(child))
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            this.activePointerId = activePointerId
            originalCapturedViewTop = capturedChild.top

            // The view has been captured, and thus a drag is about to start so stop any parents
            // intercepting
            val parent = capturedChild.parent
            parent?.requestDisallowInterceptTouchEvent(true)
        }

        override fun onViewDragStateChanged(state: Int) {
            if (listener != null) {
                listener!!.onDragStateChanged(state)
            }
        }

        override fun onViewReleased(child: View, xvel: Float, yvel: Float) {
            // Reset the active pointer ID
            activePointerId = INVALID_POINTER_ID
            val childHeight = child.height
            val targetTop: Int
            val shdsms = shouldDismiss(child, yvel)
            if (shdsms != 0) {
                targetTop =
                    if (child.top < originalCapturedViewTop) originalCapturedViewTop - childHeight else originalCapturedViewTop + childHeight
            } else {
                // Else, reset back to the original left
                targetTop = originalCapturedViewTop
            }
            if (viewDragHelper!!.settleCapturedViewAt(child.left, targetTop)) {
                ViewCompat.postOnAnimation(child, SettleRunnable(child, shdsms))
            } else if (shdsms != 0 && listener != null) {
                if (shdsms > 0)
                    listener!!.onDismissDown(child)
                else
                    listener!!.onDismissUp(child)

            }
        }


        private inner class SettleRunnable(
            private val view: View,
            private val dismiss: Int
        ) :
            Runnable {
            override fun run() {
                if (viewDragHelper != null && viewDragHelper!!.continueSettling(true)) {
                    ViewCompat.postOnAnimation(view, this)
                } else {
                    if (dismiss != 0 && listener != null) {
                        if (dismiss > 0)
                            listener!!.onDismissDown(view)
                        else
                            listener!!.onDismissUp(view)
                    }
                }
            }
        }

        private fun shouldDismiss(child: View, yvel: Float): Int {
            if (yvel != 0f) {
                if (yvel > 0)
                    return 1
                else
                    return -1
            } else {
                val distance = child.top - originalCapturedViewTop
                val thresholdDistance = Math.round(child.height * dragDismissThreshold)
                if (Math.abs(distance) >= thresholdDistance) {
                    if (distance > 0)
                        return 1
                    else
                        return -1

                }
            }
            return 0
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return child.height
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val min: Int = originalCapturedViewTop - child.height
            val max: Int = originalCapturedViewTop + child.height
            return clamp(min, top, max)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            return child.left
        }

        override fun onViewPositionChanged(child: View, left: Int, top: Int, dx: Int, dy: Int) {
            val startAlphaDistance =
                originalCapturedViewTop + child.height * alphaStartSwipeDistance
            val endAlphaDistance = originalCapturedViewTop + child.height * alphaEndSwipeDistance
            val endAlphaDistance2 = originalCapturedViewTop - child.height * alphaEndSwipeDistance
            if (top.toFloat() == startAlphaDistance) { // initial
                child.alpha = 1f
            } else if (top < startAlphaDistance) { // move up
                if (top <= endAlphaDistance2) {
                    child.alpha = 0f
                } else {
                    // We're between the start and end distances
                    val distance =
                        1f - fraction(endAlphaDistance2, startAlphaDistance, top.toFloat())
                    child.alpha = clamp(0f, 1f - distance, 1f)
                }
            } else if (top > startAlphaDistance) { // move down
                if (top >= endAlphaDistance) {
                    child.alpha = 0f
                } else {
                    // We're between the start and end distances
                    val distance = fraction(startAlphaDistance, endAlphaDistance, top.toFloat())
                    child.alpha = clamp(0f, 1f - distance, 1f)
                }
            }
        }
    }

    private fun ensureViewDragHelper(parent: ViewGroup) {
        if (viewDragHelper == null) {
            viewDragHelper = if (sensitivitySet) ViewDragHelper.create(
                parent,
                sensitivity,
                dragCallback
            ) else ViewDragHelper.create(parent, dragCallback)
        }
    }


    private fun updateAccessibilityActions(child: View) {
        ViewCompat.removeAccessibilityAction(child, AccessibilityNodeInfoCompat.ACTION_DISMISS)
        if (canSwipeDismissView(child)) {
            ViewCompat.replaceAccessibilityAction(
                child,
                AccessibilityActionCompat.ACTION_DISMISS,
                null,
                AccessibilityViewCommand { view, _ ->
                    if (canSwipeDismissView(view)) {
                        val offset = view.height
                        ViewCompat.offsetTopAndBottom(view, offset)
                        view.alpha = 0f
                        return@AccessibilityViewCommand true
                    }
                    false
                })
        }
    }

    val dragState: Int
        get() = if (viewDragHelper != null) viewDragHelper!!.viewDragState else STATE_IDLE

    companion object {
        /** A view is not currently being dragged or animating as a result of a fling/snap.  */
        const val STATE_IDLE = ViewDragHelper.STATE_IDLE


        /** Swipe direction which allows swiping in either direction.  */
        private const val DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f
        private const val DEFAULT_ALPHA_START_DISTANCE = 0f
        private const val DEFAULT_ALPHA_END_DISTANCE = DEFAULT_DRAG_DISMISS_THRESHOLD
        fun clamp(min: Float, value: Float, max: Float): Float {
            return Math.min(Math.max(min, value), max)
        }

        fun clamp(min: Int, value: Int, max: Int): Int {
            return Math.min(Math.max(min, value), max)
        }

        /** The fraction that `value` is between `startValue` and `endValue`.  */
        fun fraction(startValue: Float, endValue: Float, value: Float): Float {
            return (value - startValue) / (endValue - startValue)
        }
    }
}
