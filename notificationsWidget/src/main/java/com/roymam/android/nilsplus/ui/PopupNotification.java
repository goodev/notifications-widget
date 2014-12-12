package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roymam.android.common.SysUtils;
import com.roymam.android.notificationswidget.NiLSAccessibilityService;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.NotificationsService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class PopupNotification implements View.OnTouchListener {
    private final String TAG = PopupNotification.class.getName();
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
    private View mView;
    private WindowManager.LayoutParams mLayoutParams;
    private static List<PopupNotification> queue = new ArrayList<PopupNotification>();
    private final long mAnimationTime = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private float mTouchStartY;
    private float mTouchStartX;
    private VelocityTracker mVelocityTracker;
    private boolean mHorizontalMovement;
    private boolean mVerticalMovement;
    private int mViewWidth;
    private int mViewHeight;
    private boolean mInteractionStarted = false;

    private PopupNotification() {
        // default constructor - prevent creating this class without the "create" method
        mPrefs = null;
        mMinFlingVelocity = 0;
        mMaxFlingVelocity = 0;
        mSlop = 0;
        mNotification = null;
    }

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

        mWindowView = new RelativeLayout(context);
        mLayoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP;

        // create the popup dialog view
        LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTheme = ThemeManager.getInstance(context).getCurrentTheme();
        if (mTheme != null && mTheme.notificationLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mView = li.inflate(mTheme.notificationLayout, null);
        }
        else {
            mView = li.inflate(R.layout.notification_row, null);
        }

        // create listeners
        mWindowView.setOnTouchListener(this);
        mView.setOnTouchListener(this);

        Point displaySize = NPViewManager.getDisplaySize(mContext);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(Math.min(displaySize.x, displaySize.y), LinearLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        mWindowView.addView(mView, params);

        mPopupTimeout = 5000;
    }

    public static PopupNotification create(final Context context, final NotificationData nd)
    {
        final PopupNotification pn = new PopupNotification(context, nd);
        NotificationAdapter.applySettingsToView(context, pn.mView, nd, 0, pn.mTheme, true);
        pn.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pn.openNotification();
            }
        });
        return pn;
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
        wm.addView(mWindowView, mLayoutParams);
        mView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mView.getViewTreeObserver().removeOnPreDrawListener(this);
                mView.setTranslationY(-mView.getHeight());
                mView.animate()
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

        mView.animate()
                .translationY(-mView.getHeight())
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();

        switch(action) {
            case MotionEvent.ACTION_OUTSIDE:
                // if the user start interacting with the popup and then clicked outside of it - hide it
                if (mInteractionStarted)
                    hide();
                return false;
            case MotionEvent.ACTION_DOWN:
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

                if (!mVerticalMovement && Math.abs(currX - mTouchStartX) > mSlop) mHorizontalMovement = true;
                if (!mHorizontalMovement && Math.abs(currY - mTouchStartY) > mSlop) mVerticalMovement = true;

                if (mVerticalMovement) {
                    if (currY < mTouchStartY) {
                        mView.setTranslationY(currY - mTouchStartY);
                    } else
                        mView.setTranslationY(0);
                }
                else if (mHorizontalMovement) {
                    mView.setTranslationX(currX - mTouchStartX);
                    mView.setAlpha(1 - Math.abs(currX - mTouchStartX) / mViewWidth);
                }

                break;
            case MotionEvent.ACTION_UP:
                currY = event.getRawY();
                currX = event.getRawX();
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float velocityY = mVelocityTracker.getYVelocity();
                float deltaX = currX - mTouchStartX;
                float deltaY = currY - mTouchStartY;

                if (mVerticalMovement) {

                    if (currY < mTouchStartY) {
                        mView.setTranslationY(deltaY);
                    } else
                        mView.setTranslationY(0);


                    if (velocityY > mMinFlingVelocity && velocityY < mMaxFlingVelocity ||
                        deltaY < - mViewHeight / 2)
                        hide();
                    else
                        mView.animate().translationY(0).setListener(null);
                }
                else if (mHorizontalMovement) {
                    if (velocityX > mMinFlingVelocity && velocityX < mMaxFlingVelocity ||
                        Math.abs(deltaX) > mViewWidth / 2)
                        mView.animate().translationX(mViewWidth).alpha(0).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // clear notification
                                NotificationsService ns = NotificationsService.getSharedInstance();
                                if (ns != null)
                                    ns.clearNotification(mNotification.getUid());
                                // hide popup
                                hide();
                            }
                        });
                    else
                        mView.animate().translationX(0).alpha(1).setListener(null);
                }
                else {
                    // no horizontal or vertical movement - it is a click
                    openNotification();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                // reset position and opacity
                mView.animate().translationX(0).translationY(0).alpha(1).setListener(null);
                break;
        }

        return false;
    }
}
