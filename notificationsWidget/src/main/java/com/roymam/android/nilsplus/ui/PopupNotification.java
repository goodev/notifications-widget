package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.common.SysUtils;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class PopupNotification {
    private final int mIconSize;
    private final int mMaxIconSize;
    private final View mPreviewViewActionBar;
    private final Button mPreviewViewActionButton1;
    private final Button mPreviewViewActionButton2;
    private final Button mPreviewViewQuickReplyButton;
    private final EditText mPreviewViewQuickReplyText;
    private final View mQuickReplyBox;
    private final Point mScreenSize;
    private View mPreviewViewIcon;
    private int mMaxPreviewHeight = 0;
    private int mMinPreviewHeight = 0;
    private final static String TAG = PopupNotification.class.getName();
    private final SharedPreferences mPrefs;
    private final int mSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    private final NotificationData mNotification;
    private int mPopupTimeout;
    private Handler mHandler;
    private Theme mTheme;
    private Context mContext;
    private boolean mVisible = false;
    private RelativeLayout mWindowView;
    private View mNotificationView;
    private View mPreviewView;

    private final WindowManager.LayoutParams mLayoutParamsNoFocus;
    private WindowManager.LayoutParams mLayoutParams;
    private static List<PopupNotification> queue = new ArrayList<>();
    private final long mAnimationTime = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private boolean mInteractionStarted = false;
    private boolean mIsPreviewVisible = false;
    private int mActionBarHeight = 0;
    private boolean mIsSoftKeyVisible = false;

    private PopupNotification(Context context, NotificationData nd) {
        Log.d(TAG, "PopupNotification");
        mContext = context;
        mNotification = nd;
        mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // read default android constants
        ViewConfiguration vc = ViewConfiguration.get(mContext);
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mScreenSize = NPViewManager.getDisplaySize(context);

        mIconSize = BitmapUtils.dpToPx(mPrefs.getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE));
        mMaxIconSize = mContext.getResources().getDimensionPixelOffset(R.dimen.notification_icon_size_large);

        mWindowView = new RelativeLayout(context);

        // not focus layout
        mLayoutParamsNoFocus = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        mLayoutParamsNoFocus.gravity = Gravity.TOP;

        // focused layout with keyboard support
        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        mLayoutParams.gravity = Gravity.TOP;
        mLayoutParams.softInputMode =   WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|
                                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        // create the popup dialog view
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTheme = ThemeManager.getInstance(context).getCurrentTheme();
        if (mTheme != null && mTheme.notificationLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mNotificationView = li.inflate(mTheme.notificationLayout, null);
            mPreviewView = li.inflate(mTheme.previewLayout, null);
            mPreviewViewIcon = mPreviewView.findViewById(mTheme.customLayoutIdMap.get("notification_bg"));
            mPreviewViewActionBar = mPreviewView.findViewById(mTheme.customLayoutIdMap.get("notification_actions"));
            mPreviewViewActionButton1 = (Button)  mPreviewView.findViewById(mTheme.customLayoutIdMap.get("action_1_button"));
            mPreviewViewActionButton2 = (Button) mPreviewView.findViewById(mTheme.customLayoutIdMap.get("action_2_button"));
            mQuickReplyBox = mPreviewView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_box"));
            mPreviewViewQuickReplyButton = (Button) mPreviewView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_button"));
            mPreviewViewQuickReplyText = (EditText) mPreviewView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_text"));
        }
        else {
            mNotificationView = li.inflate(R.layout.notification_row, null);
            mPreviewView = li.inflate(R.layout.notification_preview, null);
            mPreviewViewIcon = mPreviewView.findViewById(R.id.notification_bg);
            mPreviewViewActionBar = mPreviewView.findViewById(R.id.notification_actions);
            mPreviewViewActionButton1 = (Button)  mPreviewView.findViewById(R.id.customAction1);
            mPreviewViewActionButton2 = (Button) mPreviewView.findViewById(R.id.customAction2);
            mQuickReplyBox = mPreviewView.findViewById(R.id.quick_reply_box);
            mPreviewViewQuickReplyButton = (Button) mPreviewView.findViewById(R.id.quick_reply_button);
            mPreviewViewQuickReplyText = (EditText) mPreviewView.findViewById(R.id.quick_reply_text);

        }

        // make notification go away when the user touch outside of it
        mWindowView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                    // if the user start interacting with the popup and then clicked outside of it - hide it
                    if (mInteractionStarted)
                        hide();
                }
                return false;
            }
        });

        mNotificationView.setOnTouchListener(new NotificationTouchListener());
        mPreviewView.setOnTouchListener(new PreviewTouchListener());

        Point displaySize = NPViewManager.getDisplaySize(mContext);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Math.min(displaySize.x, displaySize.y), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mWindowView.addView(mPreviewView, params);
        mWindowView.addView(mNotificationView, params);

        // preview is initially invisible
        mPreviewView.setVisibility(View.INVISIBLE);

        mPopupTimeout = 5000;
    }

    public static PopupNotification create(final Context context, final NotificationData nd)
    {
        final PopupNotification pn = new PopupNotification(context, nd);
        pn.populate(nd);
        return pn;
    }

    private void populate(NotificationData nd) {
        // apply appearance settings to the views
        NotificationAdapter.applySettingsToView(mContext, mNotificationView, nd, 0, mTheme, true);
        NotificationAdapter.applySettingsToView(mContext, mPreviewView, nd, 0, mTheme, true, true);

        // set up action listeners
        mNotificationView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openNotification();
            }
        });
        mPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotification();
            }
        });

        populateActionButton(mPreviewViewActionButton1, nd.actions, 0);
        populateActionButton(mPreviewViewActionButton2, nd.actions, 1);
        mPreviewViewQuickReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mNotification.getQuickReplyAction().actionIntent.send(mContext, 0, mNotification.getQuickReplyActioIntent(mPreviewViewQuickReplyText.getText()));
                    NotificationsService.getSharedInstance().clearNotification(mNotification.uid);
                    hide();
                    hideSoftKeyboard();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });

        // calculate min/max size for preview
        mPreviewViewIcon.getLayoutParams().width = mMaxIconSize;
        mPreviewViewIcon.getLayoutParams().height = mMaxIconSize;
        mPreviewView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mNotificationView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mPreviewViewActionBar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        mMaxPreviewHeight = mPreviewView.getMeasuredHeight();
        if (mMaxPreviewHeight > Math.min(mScreenSize.x, mScreenSize.y))
            mMaxPreviewHeight = Math.min(mScreenSize.x, mScreenSize.y);

        mMinPreviewHeight = mNotificationView.getMeasuredHeight();
        mActionBarHeight = mPreviewViewActionBar.getMeasuredHeight();
    }

    private void populateActionButton(Button button, final NotificationData.Action[] actions, final int i) {
        if (actions != null && actions.length > i) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        actions[i].actionIntent.send();
                        dismiss();
                        hide();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "failed to perform action " + actions[i].title);
                    }
                }
            });
        }
    }

    private void changeWindowHeight(int height) {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams.height = height;
        wm.updateViewLayout(mWindowView, mLayoutParams);
    }

    private void openNotification() {
        try {
            hide();
            if (mNotification != null && mNotification.getAction() != null) mNotification.getAction().send();
            else if (mNotification != null) {
                Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(mNotification.getPackageName());
                mContext.startActivity(launchIntent);
            }
        } catch (PendingIntent.CanceledException e) {
            // opening notification failed, try to open the app
            try
            {
                Intent launchIntent = mContext.getPackageManager().getLaunchIntentForPackage(mNotification.getPackageName());
                mContext.startActivity(launchIntent);
            }
            catch(Exception e2)
            {
                // cannot launch intent - do nothing...
                e2.printStackTrace();
                Toast.makeText(mContext, "Error - cannot launch app:" + mNotification.getPackageName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public PopupNotification show() {
        if (mVisible) {
            Log.d(TAG, "PopupNotification already visible");
            return this;
        }

        // do not popup when the device is locked and lock screen notifications are visible
        if (mPrefs.getBoolean(SettingsManager.FP_ENABLED, SettingsManager.DEFAULT_FP_ENABLED) &&
                SysUtils.getInstance(mContext).isLockscreenAppActive())
        {
            Log.d(TAG, "Lockscreen is active - won't popup notification");
            return this;
        }

        queue.add(0, this);

        if (queue.size() == 1)
            popupNotification();

        return this;
    }

    private void popupNotification() {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParamsNoFocus.height = mMinPreviewHeight;
        wm.addView(mWindowView, mLayoutParamsNoFocus);
        mNotificationView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mNotificationView.getViewTreeObserver().removeOnPreDrawListener(this);
                mNotificationView.setTranslationY(-mMinPreviewHeight);
                mNotificationView.animate()
                        .translationY(0)
                        .setDuration(mAnimationTime)
                        .setListener(null);
                mVisible = true;

                mHandler.postDelayed(mHideRunnable, mPopupTimeout);

                return true;
            }
        });
    }

    public PopupNotification hide() {
        if (!mVisible) {
            Log.d(TAG, "PopupNotification is not visible");
            return this;
        }

        View v = mNotificationView;
        if (mIsPreviewVisible)
            v = mPreviewView;

        v.animate()
                .translationY(-v.getHeight())
                .setDuration(mAnimationTime)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                        try {
                            wm.removeView(mWindowView);
                            queue.remove(queue.size()-1);
                            if (queue.size()>0)
                                queue.get(queue.size()-1).popupNotification();
                        }
                        catch(Exception exp)
                        {
                            // something went wrong - but we don't want to interrupt the user
                            exp.printStackTrace();
                        };
                        mVisible = false;
                    }
                });

        return this;
    }

    public boolean isVisible()
    {
        return mVisible;
    }

    public void resize(final View v, final int w, final int h, Animation.AnimationListener listener) {
        final boolean changeWidth = w != v.getLayoutParams().width;
        final boolean changeHeight = h != v.getLayoutParams().height;

        v.measure(v.getLayoutParams().width, v.getLayoutParams().height);
        final int startWidth = v.getLayoutParams().width >= 0? v.getLayoutParams().width : v.getMeasuredWidth();
        final int startHeight = v.getLayoutParams().height >= 0? v.getLayoutParams().height : v.getMeasuredHeight();

        v.measure(w, h);
        final int targetWidth = w>0?w:v.getMeasuredWidth();
        final int targetHeight = h>0?h:v.getMeasuredHeight();

        //Log.d(TAG, String.format("expand view:%d startWidth:%d startHeight:%d targetWidth:%d targetHeight:%d", v.getId(), startWidth, startHeight, targetWidth, targetHeight));

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int prevw = v.getLayoutParams().width;
                int prevh = v.getLayoutParams().height;

                if (changeWidth)
                    v.getLayoutParams().width = interpolatedTime == 1
                            ? w
                            : startWidth +  (int)((targetWidth - startWidth) * interpolatedTime);

                if (changeHeight)
                    v.getLayoutParams().height = interpolatedTime == 1
                            ? h
                            : startHeight +  (int)((targetHeight - startHeight) * interpolatedTime);

                if (prevw != v.getLayoutParams().width ||
                        prevh != v.getLayoutParams().height) {
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(mAnimationTime);
        a.setAnimationListener(listener);
        v.startAnimation(a);
    }

    private void showSoftKeyboard() {
        mPreviewViewQuickReplyText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mPreviewViewQuickReplyText, InputMethodManager.SHOW_IMPLICIT);
        mgr.restartInput(mPreviewViewQuickReplyText);
        mIsSoftKeyVisible = true;
    }

    public void hideSoftKeyboard() {
        InputMethodManager mgr = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mPreviewViewQuickReplyText.getWindowToken(), 0);
        mIsSoftKeyVisible = false;
    }

    public boolean ismIsSoftKeyVisible() {
        return mIsSoftKeyVisible;
    }

    private abstract class MovementDetectorOnTouchListener implements View.OnTouchListener {
        private boolean mHorizontalMovement;
        private boolean mVerticalMovement;

        protected View mView;
        protected float mTouchStartY;
        protected float mTouchStartX;
        protected VelocityTracker mVelocityTracker;
        protected int mViewWidth;
        protected int mViewHeight;


        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mView = v;

                    // cancel hide action
                    mHandler.removeCallbacks(mHideRunnable);

                    // store initial movement values
                    mTouchStartY = event.getRawY();
                    mTouchStartX = event.getRawX();
                    mHorizontalMovement = false;
                    mVerticalMovement = false;
                    mViewWidth = mView.getWidth();
                    mViewHeight = mView.getHeight();
                    mInteractionStarted = true;


                    return true;
                case MotionEvent.ACTION_MOVE:
                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);

                    float currY = event.getRawY();
                    float currX = event.getRawX();

                    if (!mVerticalMovement && Math.abs(currX - mTouchStartX) > mSlop)
                        mHorizontalMovement = true;
                    if (!mHorizontalMovement && Math.abs(currY - mTouchStartY) > mSlop)
                        mVerticalMovement = true;

                    if (mVerticalMovement) {
                        onVerticalMovement(currY, mTouchStartY);
                    } else if (mHorizontalMovement) {
                        onHorizontalMovement(currX, mTouchStartX);
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    currY = event.getRawY();
                    currX = event.getRawX();
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float velocityX = mVelocityTracker.getXVelocity();
                    float velocityY = mVelocityTracker.getYVelocity();

                    if (mVerticalMovement) {
                        boolean fling = velocityY > mMinFlingVelocity && velocityY < mMaxFlingVelocity;

                        onVerticalMovementEnded(currY, mTouchStartY, fling);
                    } else if (mHorizontalMovement) {
                        boolean fling = velocityX > mMinFlingVelocity && velocityX < mMaxFlingVelocity;

                        onHorizontalMovementEnded(currX, mTouchStartX, fling);
                    } else {
                        onClick();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                    onCancel();
                    break;
            }

            return false;
        }

        protected abstract void onVerticalMovement(float currY, float originalY);
        protected abstract void onHorizontalMovement(float currX, float originalX);
        protected abstract void onHorizontalMovementEnded(float currX, float originalX, boolean isFling);
        protected abstract void onVerticalMovementEnded(float currY, float originalY, boolean isFling);
        protected abstract void onClick();
        protected abstract void onCancel();
    }

    private abstract class SwipeToDismissMovementOnTouchListener extends MovementDetectorOnTouchListener {
        @Override
        protected void onHorizontalMovement(float currX, float originalX) {
            mView.setTranslationX(currX - originalX);
            mView.setAlpha(1 - Math.abs(currX - mTouchStartX) / (mViewWidth/2));
        }

        @Override
        protected void onHorizontalMovementEnded(float currX, float originalX, boolean isFling) {
            final float deltaX = currX - originalX;

            if (isFling || Math.abs(deltaX) > mViewWidth / 4) {
                int targetX = mViewWidth;
                if (deltaX < 0) targetX = -mViewWidth ;
                mView.animate().translationX(targetX).alpha(0).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onDismiss(deltaX > 0);
                    }
                });
            }
            else
                mView.animate().translationX(0).alpha(1).setListener(null);
        }

        protected abstract void onDismiss(boolean dismissRight);

        @Override
        protected void onVerticalMovement(float currY, float originalY) {
            if (currY < originalY) {
                mView.setTranslationY(currY - originalY);
            } else {
                mView.setTranslationY(0);
            }
        }

        @Override
        protected void onVerticalMovementEnded(float currY, float originalY, boolean isFling) {
            // the notification was swiped up - hide it
            float deltaY = currY - originalY;

            if (isFling && deltaY < 0 || deltaY < - mViewHeight / 2)
                onHide();
                // the notification was swiped up - but not enough - bring it back to the original position
            else if (deltaY < 0) {
                mView.animate().translationY(0).setDuration(mAnimationTime).setListener(null);
            }
        }

        protected abstract void onHide();

        @Override
        protected void onCancel() {
            // reset position and opacity
            mView.animate().translationX(0).translationY(0).alpha(1).setListener(null);
        }
    }

    private class NotificationTouchListener extends SwipeToDismissMovementOnTouchListener {
        @Override
        protected void onVerticalMovement(float currY, float originalY) {
            super.onVerticalMovement(currY, originalY);

            if (currY > originalY) {
                // the user swipes down - show a preview
                float deltaY = currY - originalY;
                if (deltaY > mSlop) {
                    if (!mIsPreviewVisible) {
                        // show the preview
                        mPreviewView.setVisibility(View.VISIBLE);
                        mPreviewViewIcon.getLayoutParams().width = mIconSize;
                        mPreviewViewIcon.getLayoutParams().height = mIconSize;
                        mPreviewView.getLayoutParams().height = mMinPreviewHeight;
                        mPreviewViewActionBar.getLayoutParams().height = 0;

                        mPreviewView.requestLayout();

                        // set maximal height for the window
                        changeWindowHeight(mMaxPreviewHeight);

                        mIsPreviewVisible = true;

                        mView.setVisibility(View.INVISIBLE);

                    } else { // preview is already visible
                        if (currY > mPreviewView.getTop() + mMinPreviewHeight) {
                            int previewHeight = (int) (currY - mPreviewView.getTop());
                            if (previewHeight > mMaxPreviewHeight)
                                previewHeight = mMaxPreviewHeight;

                            if (mPreviewView.getLayoutParams().height != previewHeight) {
                                mPreviewView.getLayoutParams().height = previewHeight;

                                int iconSize = mIconSize + (mMaxIconSize - mIconSize) * (previewHeight - mMinPreviewHeight) / (mMaxPreviewHeight - mMinPreviewHeight);

                                int actionBarHeight = Math.min(mActionBarHeight, (previewHeight - mMinPreviewHeight));

                                mPreviewViewIcon.getLayoutParams().width = iconSize;
                                mPreviewViewIcon.getLayoutParams().height = iconSize;
                                mPreviewViewActionBar.getLayoutParams().height = actionBarHeight;
                                mPreviewView.requestLayout();
                            }
                        } else {
                            if (mPreviewView.getLayoutParams().height != mMinPreviewHeight) {
                                mPreviewView.getLayoutParams().height = mMinPreviewHeight;
                                mPreviewViewIcon.getLayoutParams().width = mIconSize;
                                mPreviewViewIcon.getLayoutParams().height = mIconSize;
                                mPreviewViewActionBar.getLayoutParams().height = 0;
                                mPreviewView.requestLayout();
                            }
                        }
                    }
                } else {
                    if (mIsPreviewVisible) {
                        showNotificationCompact();
                    }
                }
            }
        }

        private void showNotificationCompact() {
            // show back the standard view
            mView.setVisibility(View.VISIBLE);
            mPreviewView.setVisibility(View.INVISIBLE);
            mPreviewViewIcon.getLayoutParams().width = mIconSize;
            mPreviewViewIcon.getLayoutParams().height = mIconSize;
            mPreviewViewActionBar.getLayoutParams().height = 0;

            mIsPreviewVisible = false;

            // set minimal height for the window
            changeWindowHeight(mMinPreviewHeight);
        }

        @Override
        protected void onVerticalMovementEnded(float currY, float originalY, boolean isFling) {
            super.onVerticalMovementEnded(currY, originalY, isFling);

            float deltaY = currY - originalY;

            // handle preview collapsing
            if (mIsPreviewVisible) {
                // if the notification was swiped down
                if (deltaY > 0) {
                    // if it was fast enough or long enough - show the preview
                    if (isFling || deltaY > mMaxPreviewHeight / 3) {
                        // complete the view expand
                        resize(mPreviewView, mPreviewView.getLayoutParams().width, mMaxPreviewHeight, null);
                        resize(mPreviewViewIcon, mMaxIconSize, mMaxIconSize, null);
                        resize(mPreviewViewActionBar, mPreviewViewActionBar.getLayoutParams().width, mActionBarHeight, null);

                        // show soft keyboard if the quick reply box is visible
                        if (mQuickReplyBox.getVisibility() == View.VISIBLE)
                            showSoftKeyboard();
                    } else {
                        // otherwise - return it the original size
                        resize(mPreviewViewIcon, mIconSize, mIconSize, null);
                        resize(mPreviewViewActionBar, mPreviewViewActionBar.getLayoutParams().width, 0, null);
                        resize(mPreviewView, mPreviewView.getLayoutParams().width, mMinPreviewHeight, new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                showNotificationCompact();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                    }
                }
            }
        }

        @Override
        protected void onDismiss(boolean dismissRight) {
            dismiss();
            // hide popup
            hide();
        }

        @Override
        protected void onHide() {
            hide();
        }

        @Override
        protected void onClick() {
            openNotification();
        }

        @Override
        protected void onCancel() {

        }
    }

    private void dismiss() {
        // clear notification
        NotificationsService ns = NotificationsService.getSharedInstance();
        if (ns != null)
            ns.clearNotification(mNotification.getUid());
    }

    private class PreviewTouchListener extends SwipeToDismissMovementOnTouchListener {
        @Override
        protected void onDismiss(boolean dismissRight) {
            // clear notification
            NotificationsService ns = NotificationsService.getSharedInstance();
            if (ns != null)
                ns.clearNotification(mNotification.getUid());
            // hide popup
            hide();
            hideSoftKeyboard();
        }

        @Override
        protected void onHide() {
            hide();
            hideSoftKeyboard();
        }

        @Override
        protected void onClick() {
            hideSoftKeyboard();
            openNotification();
        }
    }
}
