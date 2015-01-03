package com.roymam.android.nilsplus.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.RemoteInput;
import android.support.v7.widget.CardView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.roymam.android.common.BitmapUtils;
import com.roymam.android.nilsplus.ui.theme.Theme;
import com.roymam.android.nilsplus.ui.theme.ThemeManager;
import com.roymam.android.notificationswidget.NotificationData;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.notificationswidget.SettingsManager;

import static java.lang.Math.abs;

public class PreviewNotificationView extends RelativeLayout {
    private static final String TAG = PreviewNotificationView.class.getSimpleName();
    private final SharedPreferences prefs;
    private ImageButton mQuickReplySendButton;
    private TextView mQuickReplyLabel;
    private View mPreviewNotificationView;
    private View mPreviewBackground;
    private View mQuickReplyBox;
    private EditText mQuickReplyText;
    private ImageView mAppIconBGImage;
    private TextView mPreviewTitle;
    private TextView mPreviewText;
    private ImageView mPreviewIcon;
    private TextView mPreviewTime;
    private int mAnimationDuration;
    private DotsSwipeView mDotsView;
    private View mPreviewIconBG;
    //private ScrollView mScrollView;
    private ImageView mPreviewIconImageBG;
    private ImageView mPreviewIconImageFG;
    private View mNotificationContent;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private ImageView mPreviewBigPicture;
    private Theme mTheme;
    private ImageView mAppIconImage;
    private Context context;
    private int mTouchSlop;
    private int mViewWidth;
    private boolean mTouch;
    private NotificationData ni;
    private Callbacks mCallbacks;
    private VelocityTracker mVelocityTracker;
    private int mPrimaryTextColor;
    private int mNotificationBGColor;
    private boolean mIsSwipeToOpenEnabled;
    private int mLastPosY = 0;
    private int mLastPosX = 0;
    private int mLastSizeX = 0;
    private int mLastSizeY = 0;
    private int mPreviewIconSize;
    private int mStatusBarHeight;
    private boolean mVerticalDrag;
    private boolean mHorizontalDrag;
    private boolean mIgnoreTouch;

    private boolean mIsSoftKeyVisible = false;
    private Rect mStartRect;
    private int mIconSize;
    private OnInteractListener mInteractListener = null;

    public void updateSizeAndPosition(Point pos, Point size)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrimaryTextColor = prefs.getInt(SettingsManager.PRIMARY_TEXT_COLOR, SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR);
        mPreviewIconSize = prefs.getInt(SettingsManager.PREVIEW_ICON_SIZE, SettingsManager.DEFAULT_PREVIEW_ICON_SIZE);
        mIconSize = prefs.getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x, size.y);
        params.leftMargin = pos.x;
        params.topMargin = 0;

        mLastPosX = pos.x;
        mLastPosY = pos.y;
        mLastSizeX = size.x;
        mLastSizeY = size.y;


        mStatusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
        {
            mStatusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        mPreviewNotificationView.setLayoutParams(params);
        requestLayout();
    }

    public void prepareQuickReplyBox() {
        if (ni.getQuickReplyAction() != null &&
                mQuickReplyBox != null && prefs.getBoolean(SettingsManager.SHOW_QUICK_REPLY_ON_PREVIEW, SettingsManager.DEFAULT_SHOW_QUICK_REPLY_ON_PREVIEW)) {
            mQuickReplyBox.setVisibility(View.VISIBLE);
            mQuickReplyBox.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
            mQuickReplyBox.requestLayout();

            // scroll the text down again if there is additional text (because the textbox hide part of it)
            if (ni.additionalText != null) {
                //mScrollView.fullScroll(View.FOCUS_DOWN);
            }

            mQuickReplyLabel.setText(ni.getQuickReplyAction().title);
            mQuickReplyText.setText("");
        }
    }

    public void showQuickReplyBox() {
        if (ni.getQuickReplyAction() != null &&
            mQuickReplyBox != null && prefs.getBoolean(SettingsManager.SHOW_QUICK_REPLY_ON_PREVIEW, SettingsManager.DEFAULT_SHOW_QUICK_REPLY_ON_PREVIEW)) {
            mQuickReplyBox.getLayoutParams().height = 0;
            mQuickReplyBox.requestLayout();

            expand(mQuickReplyBox, mQuickReplyBox.getLayoutParams().width, LayoutParams.WRAP_CONTENT, new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    scrollDown();
                    showSoftKeyboard();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            showSoftKeyboard();
        }
    }

    private void scrollDown() {
        // make sure the conversation is fully scrolled down before re-drawing the view again
        mPreviewNotificationView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mPreviewNotificationView.getViewTreeObserver().removeOnPreDrawListener(this);
                // full scroll down - if there is a conversation content
                if (ni.additionalText != null) {
                    //mScrollView.fullScroll(View.FOCUS_DOWN);
                }
                return true;
            }
        });
    }

    public void hideQuickReplyBox() {
        if (mQuickReplyBox != null)
        {
            collapse(mQuickReplyBox, mQuickReplyBox.getLayoutParams().width, 0, null);
            //mQuickReplyBox.setVisibility(View.GONE);
            hideSoftKeyboard();
        }
    }

    public void updateContent(NotificationData newPreview) {
        setContent(newPreview, mCallbacks);
        mPreviewNotificationView.requestLayout();
    }

    public void reloadAppearance() {
        mTheme = ThemeManager.getInstance(context).getCurrentTheme();

        if (mPreviewNotificationView != null)
            removeView(mPreviewNotificationView);

        // build view from resource
        LayoutInflater inflater = LayoutInflater.from(context);
        if (mTheme != null && mTheme.previewLayout != null) {
            ThemeManager.getInstance(context).reloadLayouts(mTheme);
            mPreviewNotificationView = inflater.inflate(mTheme.previewLayout, null);
        }
        else
            mPreviewNotificationView = inflater.inflate(R.layout.notification_preview, null);

        addView(mPreviewNotificationView, new RelativeLayout.LayoutParams(mLastSizeX, mLastSizeY));

        if (mTheme != null && mTheme.previewLayout != null)
            mPreviewBackground = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("full_notification"));
        else
            mPreviewBackground = mPreviewNotificationView.findViewById(R.id.full_notification);

        // get fields
        if (mTheme != null && mTheme.previewLayout != null) {
            mNotificationContent = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_body"));
            mPreviewTitle = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_title"));
            mPreviewText = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_text"));
            mPreviewIconBG = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_bg"));
            mPreviewIcon = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_image"));
            mPreviewIconImageBG = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("icon_bg"));
            mPreviewIconImageFG = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("icon_fg"));
            mPreviewTime = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_time"));
            //mScrollView = (ScrollView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_text_scrollview"));
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("notification_big_picture"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon") != 0)
                mAppIconImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("app_icon_bg") != 0)
                mAppIconBGImage = (ImageView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("app_icon_bg"));

            if (mTheme.customLayoutIdMap != null && mTheme.customLayoutIdMap.get("quick_reply_box") != 0) {
                mQuickReplyBox = mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_box"));
                mQuickReplyText = (EditText) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_text"));
                mQuickReplyLabel = (TextView) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_label"));
                mQuickReplySendButton = (ImageButton) mPreviewNotificationView.findViewById(mTheme.customLayoutIdMap.get("quick_reply_button"));
            }
        } else {
            mNotificationContent = mPreviewNotificationView.findViewById(R.id.notification_body);
            mPreviewTitle = (TextView) mPreviewNotificationView.findViewById(R.id.notification_title);
            mPreviewText = (TextView) mPreviewNotificationView.findViewById(R.id.notification_text);
            mPreviewIconBG = mPreviewNotificationView.findViewById(R.id.notification_bg);
            mPreviewIcon = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_image);
            mPreviewIconImageBG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_bg);
            mPreviewIconImageFG = (ImageView) mPreviewNotificationView.findViewById(R.id.icon_fg);
            mPreviewTime = (TextView) mPreviewNotificationView.findViewById(R.id.notification_time);
            //mScrollView = (ScrollView) mPreviewNotificationView.findViewById(R.id.notification_text_scrollview);
            mPreviewBigPicture = (ImageView) mPreviewNotificationView.findViewById(R.id.notification_big_picture);
            mQuickReplyBox = mPreviewNotificationView.findViewById(R.id.quick_reply_box);
            mQuickReplyText = (EditText) mPreviewNotificationView.findViewById(R.id.quick_reply_text);
            mQuickReplyLabel = (TextView) mPreviewNotificationView.findViewById(R.id.quick_text_label);
            mQuickReplySendButton = (ImageButton) mPreviewNotificationView.findViewById(R.id.quick_reply_button);
        }

        prepareListeners();
    }

    public interface Callbacks
    {
        public void onDismiss(NotificationData ni);
        public void onOpen(NotificationData ni);
        public void onClick();
        public void onAction(NotificationData ni, int actionPos);
    }

    public interface OnInteractListener
    {
        public void onHideSoftkey();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev)
    {
        // handling touching the view without interfering with the standard touch handling by the scrollview, textbox, etc..
        if (handleTouch(this, ev))
            return true;
        else
            return super.dispatchTouchEvent(ev);
    }

    public PreviewNotificationView(final Context ctxt, Point size, Point pos, DotsSwipeView dotsView)
    {
        super(ctxt);
        this.context = ctxt;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);

        mDotsView = dotsView;

        mLastSizeX = size.x;
        mLastSizeY = size.y;
        reloadAppearance();

        updateSizeAndPosition(pos, size);
        hideImmediate();

        ViewConfiguration vc = ViewConfiguration.get(context);
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationDuration = Resources.getSystem().getInteger(android.R.integer.config_shortAnimTime);
    }

    public void setOnInteractListener(OnInteractListener listener)
    {
        mInteractListener = listener;
    }

    private void prepareListeners() {
        mPreviewIcon.setOnTouchListener(new OnTouchListener()
        {
            public int mActionSelected;
            private boolean mDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    mDown = true;
                    mPreviewNotificationView.animate().alpha(0).setDuration(mAnimationDuration).setListener(null);

                    // init dots view
                    int loc[] = new int[2];
                    mPreviewBackground.getLocationInWindow(loc);
                    loc[0]+=mLastPosX;
                    loc[1]+=mLastPosY;
                    int w = mPreviewBackground.getWidth();
                    int h = mPreviewBackground.getHeight();
                    mDotsView.updateSizeAndPosition(new Point(loc[0],loc[1]), new Point(w,h));
                    Rect r = new Rect(0,0, BitmapUtils.dpToPx(mPreviewIconSize), BitmapUtils.dpToPx(mPreviewIconSize));
                    mDotsView.setIcons(r, ni.getAppIcon(),
                            ni.getActions().length > 0?ni.getActions()[0].drawable:null,
                            ni.getActions().length > 1?ni.getActions()[1].drawable:null);

                    mDotsView.setVisibility(View.VISIBLE);
                    mDotsView.animate().alpha(1).setDuration(mAnimationDuration).setListener(null);
                    mDotsView.dispatchTouchEvent(event);
                    mActionSelected = -1;
                }
                else if (event.getAction() == MotionEvent.ACTION_MOVE)
                {
                    mDotsView.dispatchTouchEvent(event);
                }
                else if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    mDown = false;

                    mDotsView.dispatchTouchEvent(event);
                    mDotsView.animate().alpha(0).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter()
                    {
                        @Override
                        public void onAnimationEnd(Animator animation)
                        {
                            mDotsView.setVisibility(View.GONE);
                        }
                    });
                    mPreviewNotificationView.animate().alpha(1).setDuration(mAnimationDuration).setListener(null);
                    mActionSelected = mDotsView.getSelected();

                    if (mActionSelected == 0)
                    {
                        mCallbacks.onOpen(ni);
                    }
                    else if (mActionSelected == 1)
                    {
                        try
                        {
                            if (mCallbacks != null)
                                mCallbacks.onAction(ni, 0);
                            else
                                ni.getActions()[0].actionIntent.send();
                        } catch (PendingIntent.CanceledException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else if (mActionSelected == 2)
                    {
                        try
                        {
                            if (mCallbacks != null)
                                mCallbacks.onAction(ni, 1);
                            else
                                ni.getActions()[1].actionIntent.send();

                        } catch (PendingIntent.CanceledException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
        });

        if (mQuickReplySendButton != null)
            mQuickReplySendButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    Bundle params = new Bundle();
                    final NotificationData.Action action = ni.getQuickReplyAction();
                    params.putCharSequence(action.resultKey, mQuickReplyText.getText());

                    Intent clipIntent = new Intent();
                    clipIntent.putExtra(RemoteInput.EXTRA_RESULTS_DATA, params);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN);
                        intent.setClipData(ClipData.newIntent(RemoteInput.RESULTS_CLIP_LABEL, clipIntent));

                    //RemoteInput.addResultsToIntent(action.remoteInputs, intent, params);
                    try {
                        action.actionIntent.send(context, 0, intent);
                        hide();
                        mCallbacks.onDismiss(ni);
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    public void hideImmediate()
    {
        mPreviewNotificationView.setVisibility(View.GONE);
        setVisibility(View.GONE);
    }

    public void setSizeAndPosition(Point size, Point pos)
    {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(size.x,size.y);
        params.leftMargin = pos.x;
        params.topMargin = pos.y;
        mPreviewNotificationView.setLayoutParams(params);
    }


    public void expand(final View v, final int w, final int h, Animation.AnimationListener listener) {
        //Log.d(TAG, String.format("expand(%d, %d, %d)", v.getId(), w, h));
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
                    //Log.d(TAG, String.format("size changed: vid:%d w:%d h:%d", v.getId(), v.getLayoutParams().width, v.getLayoutParams().height));
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(mAnimationDuration);
        a.setAnimationListener(listener);
        v.startAnimation(a);
    }

    public void collapse(final View v, final int w, final int h, Animation.AnimationListener listener) {
        //Log.d(TAG, String.format("collapse(%d, %d, %d)", v.getId(), w, h));
        final boolean changeWidth = w != v.getLayoutParams().width;
        final boolean changeHeight = h != v.getLayoutParams().height;
        v.measure(v.getLayoutParams().width, v.getLayoutParams().height);
        final int startWidth = v.getMeasuredWidth();
        final int startHeight = v.getMeasuredHeight();
        v.measure(w, h);
        final int targetWidth = w>=0?w:v.getMeasuredWidth();
        final int targetHeight = h>=0?h:v.getMeasuredHeight();
        //Log.d(TAG, String.format("collapse view:%d startWidth:%d startHeight:%d targetWidth:%d targetHeight:%d", v.getId(), startWidth, startHeight, targetWidth, targetHeight));
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                int prevw = v.getLayoutParams().width;
                int prevh = v.getLayoutParams().height;

                if (changeWidth)
                    v.getLayoutParams().width = interpolatedTime == 1
                            ? w
                            : targetWidth +  (int)((startWidth - targetWidth) * (1-interpolatedTime));

                if (changeHeight)
                    v.getLayoutParams().height = interpolatedTime == 1
                            ? h
                            : targetHeight +  (int)((startHeight - targetHeight) * (1-interpolatedTime));

                if (prevw != v.getLayoutParams().width ||
                    prevh != v.getLayoutParams().height) {
                    //Log.d(TAG, String.format("size changed: vid:%d w:%d h:%d", v.getId(), v.getLayoutParams().width, v.getLayoutParams().height));
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(mAnimationDuration);
        a.setAnimationListener(listener);
        v.startAnimation(a);
    }

    public int calcOffset()
    {
        mPreviewNotificationView.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int targetHeight = mPreviewNotificationView.getMeasuredHeight();
        String yAlignment = prefs.getString(SettingsManager.VERTICAL_ALIGNMENT, SettingsManager.DEFAULT_VERTICAL_ALIGNMENT);
        int offset = mLastSizeY - targetHeight;
        if (offset  < 0) offset = 0;

        if (yAlignment.equals("center"))
        {
            offset /=2;
        }
        else if (yAlignment.equals("bottom"))
        {
            // do nothing - deltay stays the same size
        }
        else
        {
            offset  = 0;
        }
        return offset;
    }

    public void show(Rect startRect, final boolean showKeyboard)
    {
        setVisibility(View.VISIBLE);
        setAlpha(1);
        mPreviewNotificationView.setAlpha(1);
        mPreviewNotificationView.setVisibility(View.VISIBLE);
        mPreviewNotificationView.setTranslationX(0);
        mPreviewNotificationView.getLayoutParams().height = startRect.height();
        mPreviewNotificationView.requestLayout();
        mStartRect = startRect;
        mPreviewNotificationView.setTranslationY(mStartRect.top);

        mPreviewIconBG.getLayoutParams().width = BitmapUtils.dpToPx(mIconSize);
        mPreviewIconBG.getLayoutParams().height = BitmapUtils.dpToPx(mIconSize);
        mPreviewIconBG.requestLayout();

        requestLayout();

        Log.d(TAG, "showing a preview window on offset:" + calcOffset() + " startRect:" + startRect);
        mPreviewNotificationView.animate().translationY(calcOffset()).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // scroll down the text if it is a conversation mode
                scrollDown();
            }
        });

        // prepare the quick reply box to start animating with the preview expand
        if (showKeyboard) {
            prepareQuickReplyBox();
        }

        // animate opening the preview view and the preview icon
        expand(mPreviewNotificationView, mPreviewNotificationView.getLayoutParams().width, LayoutParams.WRAP_CONTENT, null);
        expand(mPreviewIconBG, BitmapUtils.dpToPx(mPreviewIconSize), BitmapUtils.dpToPx(mPreviewIconSize), null);

        // start animating the quick reply box
        if (showKeyboard) {
            showQuickReplyBox();
        }

        // if the time is not displayed on list view - animate it grows
        //if (!prefs.getBoolean(SettingsManager.SHOW_TIME, SettingsManager.DEFAULT_SHOW_TIME)) {
        //    mPreviewTime.getLayoutParams().width = 0;
        //    resize(mPreviewTime, LayoutParams.WRAP_CONTENT, mPreviewTime.getLayoutParams().height);
        //}
    }

    public void hide()
    {
        collapse(mPreviewNotificationView, mPreviewNotificationView.getLayoutParams().width, mStartRect.height(), null);
        collapse(mPreviewIconBG, BitmapUtils.dpToPx(mIconSize), BitmapUtils.dpToPx(mIconSize), null);
        hideQuickReplyBox();

        // if the time is not displayed on list view - animate it shrinks
        //if (!prefs.getBoolean(SettingsManager.SHOW_TIME, SettingsManager.DEFAULT_SHOW_TIME)) {
        //    resize(mPreviewTime, 0, mPreviewTime.getLayoutParams().height);
        //}

        mPreviewNotificationView.animate().translationY(mStartRect.top).setDuration(mAnimationDuration).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mPreviewNotificationView.setVisibility(View.GONE);
                setVisibility(View.GONE);

            }
        });
    }


    public void setContent(NotificationData ni, Callbacks callbacks)
    {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Theme theme = ThemeManager.getInstance(context).getCurrentTheme();

        // set appearance settings and theme
        mPrimaryTextColor = prefs.getInt(SettingsManager.PRIMARY_TEXT_COLOR, SettingsManager.DEFAULT_PRIMARY_TEXT_COLOR);
        int secondaryTextColor = prefs.getInt(SettingsManager.SECONDARY_TEXT_COLOR, SettingsManager.DEFAULT_SECONDARY_TEXT_COLOR);
        mNotificationBGColor = prefs.getInt(SettingsManager.MAIN_BG_COLOR, SettingsManager.DEFAULT_MAIN_BG_COLOR);
        int iconBGColor = prefs.getInt(SettingsManager.ICON_BG_COLOR, SettingsManager.DEFAULT_ICON_BG_COLOR);

        this.ni = ni;
        this.mCallbacks = callbacks;

        mPreviewTitle.setText(ni.getTitle()!=null?ni.getTitle().toString():null);
        mPreviewText.setText(ni.getText() != null ? ni.getText().toString() : null);
        mPreviewText.setMovementMethod(new ScrollingMovementMethod());
        if (ni.additionalText != null ) {
            mPreviewText.setText(ni.additionalText);
            mPreviewText.setGravity(Gravity.BOTTOM);
            //mScrollView.fullScroll(View.FOCUS_DOWN);
        }
        else
        {
            mPreviewText.setGravity(Gravity.TOP);
            //mScrollView.fullScroll(View.FOCUS_UP);
        }
        mPreviewTitle.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewText.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        mPreviewTime.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);
        if (mQuickReplyText != null) mQuickReplyText.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault);

        mPreviewTitle.setTextSize(prefs.getInt(SettingsManager.TITLE_FONT_SIZE, SettingsManager.DEFAULT_TITLE_FONT_SIZE));
        mPreviewText.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextSize(prefs.getInt(SettingsManager.TITLE_FONT_SIZE, SettingsManager.DEFAULT_TITLE_FONT_SIZE));
        if (mQuickReplyText != null) mQuickReplyText.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));

        mPreviewTime.setTextSize(prefs.getInt(SettingsManager.TEXT_FONT_SIZE, SettingsManager.DEFAULT_TEXT_FONT_SIZE));
        Bitmap icon = NotificationAdapter.createThemedIcon(ni.getIcon(), theme, (int) context.getResources().getDimension(R.dimen.notification_icon_size_large));
        mPreviewIcon.setImageDrawable(new BitmapDrawable(getResources(), icon));

        if (theme.iconBg != null)
            theme.iconBg.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);

        if (mAppIconImage != null)
        {
            Bitmap appIcon = ni.getAppIcon();
            // show app icon only if the primary icon is a large icon
            if (ni.largeIcon != null)
            {
                mAppIconImage.setImageDrawable(new BitmapDrawable(appIcon));
                if (mAppIconBGImage != null && theme.appIconBg != null)
                {
                    Drawable appIconBgDrawable = theme.appIconBg;

                    if (theme.prominentAppIconBg)
                    {
                        if (theme.appIconBg instanceof BitmapDrawable)
                        {
                            appIconBgDrawable = new BitmapDrawable(BitmapUtils.colorBitmap(((BitmapDrawable)theme.appIconBg).getBitmap(), ni.appColor));
                        }
                        else
                        {
                            Log.w(TAG, "invalid theme. prominent app icon background works only with BitmapDrawable");
                        }
                    }
                    mAppIconBGImage.setImageDrawable(appIconBgDrawable);
                }
            }
            else
            {
                if (mAppIconBGImage != null) mAppIconBGImage.setImageDrawable(null);
                mAppIconImage.setImageDrawable(null);

                // for Android L notifications - set main icon as the app icon (the small monochrome one) instead of the colored one)
                // TODO: make it optional in the theme booleans
                mPreviewIcon.setImageDrawable(new BitmapDrawable(NotificationAdapter.createThemedIcon(ni.getAppIcon(), theme, BitmapUtils.dpToPx(prefs.getInt(SettingsManager.ICON_SIZE, SettingsManager.DEFAULT_ICON_SIZE)))));
            }
        }

        Drawable iconBgImage = theme.iconBg;
        if (iconBgImage != null)
            iconBgImage.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);

        if (theme.prominentIconBg)
        {
            if (iconBgImage instanceof BitmapDrawable)
            {
                iconBgImage = new BitmapDrawable(BitmapUtils.colorBitmap(((BitmapDrawable)iconBgImage).getBitmap(), ni.appColor));
            }
            else
            {
                Log.w(TAG, "invalid theme. prominent icon background works only with BitmapDrawable");
            }
        }

        mPreviewIconImageBG.setImageDrawable(iconBgImage);
        mPreviewIconImageFG.setImageDrawable(theme.iconFg);
        if (theme.previewTextBG != null)
            theme.previewTextBG.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);
        mNotificationContent.setBackgroundDrawable(theme.previewTextBG);
        mPreviewTime.setText(ni.getTimeText(context));

        // set colors
        mPreviewTitle.setTextColor(mPrimaryTextColor);
        if (mQuickReplyLabel != null) mQuickReplyLabel.setTextColor(mPrimaryTextColor);

        if (ni.appColor != 0 && prefs.getBoolean(SettingsManager.AUTO_TITLE_COLOR, false)) {
            mPreviewTitle.setTextColor(ni.appColor);
            if (mQuickReplyLabel != null) mQuickReplyLabel.setTextColor(ni.appColor);
        }
        mPreviewText.setTextColor(secondaryTextColor);
        if (mQuickReplyText != null) mQuickReplyText.setTextColor(secondaryTextColor);

        mPreviewTime.setTextColor(secondaryTextColor);
        if (mTheme.allowOpacityChange)
            mPreviewBackground.setAlpha(prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100.0f);
        else
            mPreviewBackground.setBackgroundColor(mNotificationBGColor);

        mPreviewIconBG.setBackgroundColor(iconBGColor);

        // apply theme
        if (theme.previewBG != null)
        {
            theme.previewBG.setAlpha(255 * prefs.getInt(SettingsManager.MAIN_BG_OPACITY, SettingsManager.DEFAULT_MAIN_BG_OPACITY) / 100);
            if (!(mPreviewBackground instanceof CardView))
                mPreviewBackground.setBackgroundDrawable(theme.previewBG);
        }

        Bitmap largestBitmap = null;
        if (ni.bitmaps != null)
            for(Bitmap bitmap : ni.bitmaps)
            {
                if (largestBitmap == null)
                    largestBitmap = bitmap;
                else
                    if (largestBitmap.getHeight()*largestBitmap.getWidth() <
                        bitmap.getHeight()*bitmap.getWidth())
                        largestBitmap = bitmap;
            }

        if (largestBitmap != null &&
                (largestBitmap.getWidth() > context.getResources().getDimension(R.dimen.big_picture_min_size) ||
                 largestBitmap.getHeight() > context.getResources().getDimension(R.dimen.big_picture_min_size)))
            mPreviewBigPicture.setImageBitmap(largestBitmap);
        else
            mPreviewBigPicture.setImageBitmap(null);

        // apply font style and size if available
        if (theme.timeFontSize != -1) mPreviewTime.setTextSize(theme.timeFontSize);
        if (theme.titleTypeface != null) mPreviewTitle.setTypeface(theme.titleTypeface);
        if (theme.textTypeface != null) mPreviewText.setTypeface(theme.titleTypeface);
        if (theme.timeTypeface != null) mPreviewTime.setTypeface(theme.titleTypeface);

    }

    // touch handling
    float mTouchStartX;
    float mTouchStartY;

    private boolean isTouchHitView(View v, MotionEvent ev)
    {
        // if view is not visible - return false
        if (v == null || v.getVisibility() != View.VISIBLE) return false;

        int[] parentCords = new int[2];
        mPreviewBackground.getLocationOnScreen(parentCords);
        int x = (int)ev.getRawX() - parentCords[0];
        int y = (int)ev.getRawY() - parentCords[1];

        Rect rect = new Rect();
        v.getHitRect(rect);

        if (rect.contains(x, y)) {
            return true;
        }
        return false;
    }

    public boolean handleTouch(View v, MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            // ignore start dragging quick reply box or the notification icon
            if (isTouchHitView(mQuickReplyBox, event)) {
                showSoftKeyboard();
                mIgnoreTouch = true;
                return false;
            }
            if (isTouchHitView(mPreviewIcon,event)) {
                mIgnoreTouch = true;
                return false;
            }
            mIgnoreTouch = false;

            mTouchStartX = event.getRawX();
            mTouchStartY = event.getRawY();
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            mViewWidth = mPreviewNotificationView.getWidth();
            mTouch = true;
            mHorizontalDrag = false;
            mVerticalDrag = false;
            mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(event);

            // store pref for use later
            mIsSwipeToOpenEnabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsManager.SWIPE_TO_OPEN, SettingsManager.DEFAULT_SWIPE_TO_OPEN);
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            if (mTouch && !mIgnoreTouch)
            {
                mVelocityTracker.addMovement(event);
                float deltaX = event.getRawX() - mTouchStartX;
                float deltaY = event.getRawY() - mTouchStartY;

                if (abs(deltaX) > mTouchSlop)
                    mHorizontalDrag = true;

                if (abs(deltaY) > mTouchSlop)
                    mVerticalDrag = true;

                if (mVerticalDrag && !mHorizontalDrag)
                {
                    // cancel swipe & click - keep only scroll text
                    mTouch = false;

                    // reset horizontal dragging
                    mPreviewNotificationView.setTranslationX(0);
                    mPreviewNotificationView.setAlpha(1);

                    // pass this event so the scrollview will handle the vertical scrolling
                    return false;
                }
                if (mHorizontalDrag)
                {
                    // update position and opacity according the swiping gesture
                    mPreviewNotificationView.setTranslationX(deltaX);
                    mPreviewNotificationView.setAlpha((mViewWidth- abs(deltaX))/mViewWidth);

                    // prevent other controls receiving this event
                    return true;
                }
            }
            else // unhandled - pass it to the child views
                return false;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP)
        {
            if (mTouch && !mIgnoreTouch) {
                mTouch = false;

                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                float deltaX = event.getRawX() - mTouchStartX;
                if (abs(deltaX) > mViewWidth / 2 ||
                        mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity && absVelocityY < absVelocityX) {
                    // animate dismiss
                    int w;
                    final boolean swipeRight = (deltaX > 0);
                    if (swipeRight)
                        w = mViewWidth;
                    else
                        w = -mViewWidth;

                    // swipe animation
                    mPreviewNotificationView.animate().translationX(w).alpha(0).setDuration(mAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    // dismiss notification
                                    if (mCallbacks != null) {
                                        if (mIsSwipeToOpenEnabled && swipeRight)
                                            mCallbacks.onOpen(ni);
                                        else
                                            mCallbacks.onDismiss(ni);
                                    }
                                }
                            });
                } else // the swipe wasn't fast enough - restoring the item to the original position
                {
                    mPreviewNotificationView.animate().translationX(0).alpha(1).setDuration(mAnimationDuration).setListener(null);
                }

                // if the user actually didn't drag at all - it is a click
                if (!mHorizontalDrag && !mVerticalDrag) {
                    InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    // if soft keyboard is visible - hide it
                    if (mIsSoftKeyVisible) {
                        hideSoftKeyboard();
                    } else if (mCallbacks != null) mCallbacks.onClick();

                    // make sure other views won't get this event
                    return true;
                }
            }
            else
                // unhandled - pass it to the child views
                return false;
        }

        // unhandled - pass it to the child views
        return false;
    }

    private void showSoftKeyboard() {
        mQuickReplyText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.showSoftInput(mQuickReplyText, InputMethodManager.SHOW_IMPLICIT);
        mgr.restartInput(mQuickReplyText);
        mIsSoftKeyVisible = true;
    }

    public void hideSoftKeyboard() {
        InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mQuickReplyText.getWindowToken(), 0);
        mIsSoftKeyVisible = false;

        if (mInteractListener != null) mInteractListener.onHideSoftkey();
    }

    public boolean ismIsSoftKeyVisible() {
        return mIsSoftKeyVisible;
    }


    public void cleanup()
    {
        //mScrollView.setOnTouchListener(null);
        mPreviewNotificationView.setOnTouchListener(null);
        mPreviewIcon.setOnTouchListener(null);
        mPreviewNotificationView.setOnClickListener(null);
    }
}