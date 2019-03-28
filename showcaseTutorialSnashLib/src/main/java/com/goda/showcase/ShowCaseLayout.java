package com.goda.showcase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.goda.showcase.ShowCaseDialog.TAG;

public class ShowCaseLayout extends FrameLayout {

    // customized attribute
    private int layoutRes;
    private int textColor;
    private int titleTextColor;
    private int shadowColor;
    private float textSize;
    private float textTitleSize;
    private int spacing;
    private int arrowMargin;
    private int arrowWidth;
    private boolean useCircleIndicator;

    private boolean isCancelable;
    private boolean hasSkipWord;

    private String prevString;
    private String nextString;
    private String finishString;
    private String skipString;
    View line;
    //View line2;
    // View line3;
    private int backgroundContentColor;
    private int circleBackgroundDrawableRes;

    // View
    private ViewGroup viewGroup;
    private Bitmap bitmap;
    private View lastTutorialView;
    private Paint viewPaint;

    // listener
    private ShowCaseListener showCaseListener;

    ShowCaseContentPosition showCaseContentPosition;

    private int highlightLocX;
    private int highlightLocY;

    // determined if this is last chain
    private boolean isStart;
    private boolean isLast;

    // path for arrow
    private Path path;
    private Paint arrowPaint;
    private TextView textViewTitle;
    private TextView textViewDesc;
    private TextView prevButton;
    private LinearLayout prevButtonlin;
    private TextView nextButton;
    private LinearLayout nextButtonlin;
    private ViewGroup viewGroupIndicator;
    View prev_next_lin;
    private boolean goThroughWindowF = true;

    public ShowCaseLayout(Context context, @Nullable ShowCaseBuilder builder) {
        super(context);
        init(context, builder);
    }

    public ShowCaseLayout(Context context) {
        super(context);
        init(context, null);
    }

    public ShowCaseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public ShowCaseLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, null);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public ShowCaseLayout(Context context,
                          AttributeSet attrs,
                          int defStyleAttr,
                          int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, null);
    }

    private void init(Context context, @Nullable ShowCaseBuilder builder) {
        setVisibility(View.GONE);

        if (isInEditMode()) {
            return;
        }

        applyAttrs(context, builder);

        //setBackground, color
        initFrame();

        // setContentView
        initContent(context);

        setClickable(this.isCancelable);
        setFocusable(this.isCancelable);

        if (this.isCancelable) {
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNextClicked();
                }
            });
        }
    }

    private void onNextClicked() {
        if (showCaseListener != null) {
            if (this.isLast) {
                ShowCaseLayout.this.showCaseListener.onComplete();
            } else {
                ShowCaseLayout.this.showCaseListener.onNext();
            }
        }
    }

    private void initFrame() {
        setWillNotDraw(false);

        viewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        viewPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(backgroundContentColor);
        arrowPaint.setStyle(Paint.Style.FILL);

        setBackgroundColor(shadowColor);
    }

    public void setShowCaseListener(ShowCaseListener showCaseListener) {
        this.showCaseListener = showCaseListener;
    }

    public void showTutorial(View view,
                             String title,
                             String text,
                             int currentTutorIndex,
                             int tutorsListSize,
                             ShowCaseContentPosition showCaseContentPosition,
                             int tintBackgroundColor,
                             final int[] customTarget, final int radius) throws Throwable {
        Log.d(TAG, "showTutorial:  ");

        this.isStart = currentTutorIndex == 0;

        this.isLast = currentTutorIndex == tutorsListSize - 1;
        this.showCaseContentPosition = showCaseContentPosition;

        if (this.bitmap != null) {
            this.bitmap.recycle();
        }
        if (this.lastTutorialView != null) {
            this.lastTutorialView.setDrawingCacheEnabled(false);
        }

        if (TextUtils.isEmpty(title)) {
            textViewTitle.setVisibility(View.GONE);
        } else {
            textViewTitle.setText(fromHtml(title));
            textViewTitle.setVisibility(View.VISIBLE);
        }

        textViewDesc.setText(fromHtml(text));

        if (prevButton != null) {
            if (isStart) {
                if (hasSkipWord) {
                    prevButton.setText(skipString);
                    prevButton.setVisibility(View.VISIBLE);
                } else {
                    prevButton.setVisibility(View.INVISIBLE);
                }
            } else {
                prevButton.setText(prevString);
                prevButton.setVisibility(View.VISIBLE);
            }
        }

        if (nextButton != null) {
            if (isLast) {
                nextButton.setText(finishString);
            } else if (currentTutorIndex < tutorsListSize - 1) { // has next
                nextButton.setText(nextString);
            }
        }

        makeCircleIndicator(!isStart || !isLast, currentTutorIndex, tutorsListSize);

        if (view == null) {
            this.lastTutorialView = null;
            this.bitmap = null;
            this.highlightLocX = 0;
            this.highlightLocY = 0;
            moveViewToCenter();
        } else {
            this.lastTutorialView = view;
            if (view.willNotCacheDrawing()) {
                view.setWillNotCacheDrawing(false);
            }
            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            if (tintBackgroundColor == 0) {
                this.bitmap = view.getDrawingCache();
            } else {
                Bitmap bitmapTemp = view.getDrawingCache();

                Bitmap bigBitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                        view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                Canvas bigCanvas = new Canvas(bigBitmap);
                bigCanvas.drawColor(tintBackgroundColor);
                Paint paint = new Paint();
                bigCanvas.drawBitmap(bitmapTemp, 0f, 0f, paint);

                this.bitmap = bigBitmap;
            }

            //set custom target to view
            if (customTarget != null) {
                if (customTarget.length == 2) {
                    this.bitmap = ViewHelper.getCroppedBitmap(bitmap, customTarget, radius);
                } else if (customTarget.length == 4) {
                    this.bitmap = ViewHelper.getCroppedBitmap(bitmap, customTarget);
                }

                this.highlightLocX = customTarget[0] - radius;
                this.highlightLocY = customTarget[1] - radius;
                Log.d(TAG, "onGlobalLayout: customTarget ");

            } else { // use view location as target
                final int[] location = new int[2];
                view.getLocationInWindow(location);

                this.highlightLocX = location[0];
                this.highlightLocY = location[1] - ViewHelper.getStatusBarHeight(getContext());
                Log.d(TAG, "onGlobalLayout: getLocationInWindow ");

            }

            this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (ShowCaseLayout.this.bitmap != null) {
                        moveViewBasedHighlight(ShowCaseLayout.this.highlightLocX,
                                ShowCaseLayout.this.highlightLocY,
                                ShowCaseLayout.this.highlightLocX + ShowCaseLayout.this.bitmap.getWidth(),
                                ShowCaseLayout.this.highlightLocY + ShowCaseLayout.this.bitmap.getHeight());

                        if (Build.VERSION.SDK_INT < 16) {
                            ShowCaseLayout.this.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            ShowCaseLayout.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        invalidate();
                        Log.d(TAG, "onGlobalLayout: inavalideta ");
                    }
                }
            });
        }

        this.setVisibility(View.VISIBLE);
        Log.d(TAG, "onGlobalLayout: setVisibility ");
        prev_next_lin.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (isLast && isStart) {
                            hideprev_next_layout();
                        } else if (isLast) {
                            nextButtonlin.setVisibility(INVISIBLE);
                            prevButtonlin.setVisibility(VISIBLE);
                        } else if (isStart && !hasSkipWord) {
                            prevButtonlin.setVisibility(INVISIBLE);
                            nextButtonlin.setVisibility(VISIBLE);

                        } else {
                            line.setVisibility(VISIBLE);
                            prevButtonlin.setVisibility(VISIBLE);
                            nextButtonlin.setVisibility(VISIBLE);

                        }

                    }
                });
    }

    public void hideprev_next_layout() {
        //prev_next_lin.setVisibility(GONE);
        line.setVisibility(GONE);
        prevButtonlin.setVisibility(GONE);
        nextButtonlin.setVisibility(GONE);
    }


    public void hideTutorial() {
        this.setVisibility(View.INVISIBLE);
        Log.d(TAG, "onGlobalLayout: setVisibility invisi ");

    }

    private void makeCircleIndicator(boolean hasMoreOneCircle,
                                     int currentTutorIndex,
                                     int tutorsListSize) {
        if (useCircleIndicator && this.viewGroupIndicator != null) {
            if (hasMoreOneCircle) { // has more than 1 circle
                // already has circle indicator
                if (this.viewGroupIndicator.getChildCount() == tutorsListSize) {
                    for (int i = 0; i < tutorsListSize; i++) {
                        View viewCircle = this.viewGroupIndicator.getChildAt(i);
                        if (i == currentTutorIndex) {
                            viewCircle.setSelected(true);
                        } else {
                            viewCircle.setSelected(false);
                        }
                    }
                } else { //reinitialize, the size is different
                    this.viewGroupIndicator.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    for (int i = 0; i < tutorsListSize; i++) {
                        View viewCircle = inflater.inflate(R.layout.circle_green_view,
                                viewGroupIndicator,
                                false);
                        viewCircle.setBackgroundResource(this.circleBackgroundDrawableRes);
                        if (i == currentTutorIndex) {
                            viewCircle.setSelected(true);
                        }
                        this.viewGroupIndicator.addView(viewCircle);
                    }
                }
            } else {
                this.viewGroupIndicator.removeAllViews();
            }
        }
    }

    public void closeTutorial() {
        setVisibility(View.GONE);
        if (this.bitmap != null) {
            this.bitmap.recycle();
            this.bitmap = null;
        }
        if (lastTutorialView != null) {
            this.lastTutorialView.setDrawingCacheEnabled(false);
            this.lastTutorialView = null;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycleResources();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (bitmap == null || bitmap.isRecycled()) {
            Log.d(TAG, "onDraw: return");
            return;
        }
        super.onDraw(canvas);
        canvas.drawBitmap(this.bitmap, this.highlightLocX, this.highlightLocY, viewPaint);

        // drawArrow
        if (path != null && this.viewGroup.getVisibility() == View.VISIBLE) {
            canvas.drawPath(path, arrowPaint);
        }
    }

    private void applyAttrs(Context context, @Nullable ShowCaseBuilder builder) {
        // set Default value before check builder (might be null)
        this.layoutRes = R.layout.tutorial_view;

        this.textColor = Color.WHITE;
        this.titleTextColor = Color.WHITE;
        this.textTitleSize = getResources().getDimension(R.dimen.text_title);
        this.textSize = getResources().getDimension(R.dimen.text_normal);

        this.shadowColor = ContextCompat.getColor(context, R.color.shadowShow);
        this.spacing = (int) getResources().getDimension(R.dimen.spacing_normal);

        this.arrowMargin = this.spacing / 3;
        this.arrowWidth = (int) (1.5 * this.spacing);

        this.backgroundContentColor = Color.BLACK;
        this.circleBackgroundDrawableRes = R.drawable.selector_circle_green;

        this.prevString = getContext().getString(R.string.previousShow);
        this.nextString = getContext().getString(R.string.nextShow);
        this.finishString = getContext().getString(R.string.finishShow);
        this.skipString = getContext().getString(R.string.skipShow);

        if (builder == null) {
            return;
        }

        this.layoutRes = builder.getLayoutRes() != 0 ?
                builder.getLayoutRes()
                : this.layoutRes;

        this.textColor = builder.getTextColorRes() != 0 ?
                ContextCompat.getColor(context, builder.getTextColorRes())
                : this.textColor;

        this.titleTextColor = builder.getTitleTextColorRes() != 0 ?
                ContextCompat.getColor(context, builder.getTitleTextColorRes())
                : this.titleTextColor;

        this.textTitleSize = builder.getTitleTextSizeRes() != 0 ?
                getResources().getDimension(builder.getTitleTextSizeRes())
                : this.textTitleSize;

        this.textSize = builder.getTextSizeRes() != 0 ?
                getResources().getDimension(builder.getTextSizeRes())
                : this.textSize;

        this.backgroundContentColor = builder.getBackgroundContentColorRes() != 0 ?
                ContextCompat.getColor(context, builder.getBackgroundContentColorRes())
                : this.backgroundContentColor;

        this.shadowColor = builder.getShadowColorRes() != 0 ?
                ContextCompat.getColor(context, builder.getShadowColorRes()) :
                this.shadowColor;

        this.spacing = builder.getSpacingRes() != 0 ?
                (int) getResources().getDimension(builder.getSpacingRes())
                : this.spacing;

        this.circleBackgroundDrawableRes = builder.getCircleIndicatorBackgroundDrawableRes() != 0 ?
                builder.getCircleIndicatorBackgroundDrawableRes()
                : this.circleBackgroundDrawableRes;

        this.prevString = builder.getPrevStringRes() != 0 ?
                getContext().getString(builder.getPrevStringRes())
                : this.prevString;

        this.nextString = builder.getNextStringRes() != 0 ?
                getContext().getString(builder.getNextStringRes())
                : this.nextString;

        this.finishString = builder.getFinishStringRes() != 0 ?
                getContext().getString(builder.getFinishStringRes())
                : this.finishString;

        this.skipString = builder.getSkipStringRes() != 0 ?
                getContext().getString(builder.getSkipStringRes())
                : this.skipString;

        this.useCircleIndicator = builder.useCircleIndicator();
        this.hasSkipWord = builder.isUseSkipWord();

        this.isCancelable = builder.isClickable();

        if (builder.isUseArrow()) {
            this.arrowMargin = this.spacing / 3;
            this.arrowWidth = builder.getArrowWidth() != 0 ?
                    (int) getResources().getDimension(builder.getArrowWidth())
                    : this.arrowWidth;
        } else {
            this.arrowMargin = 0;
            this.arrowWidth = 0;
        }

    }

    private void initContent(final Context context) {
        this.viewGroup = (ViewGroup)
                LayoutInflater.from(context).inflate(this.layoutRes, this, false);
        Log.d(TAG, "initContent: ");
        View viewGroupTutorContent = viewGroup.findViewById(R.id.view_group_tutor_content);
        prev_next_lin = viewGroup.findViewById(R.id.prev_next_lin);

        ViewHelper.setBackgroundColor(viewGroupTutorContent, this.backgroundContentColor);

        textViewTitle = (TextView) viewGroupTutorContent.findViewById(R.id.text_title);

        textViewTitle = (TextView) viewGroupTutorContent.findViewById(R.id.text_title);
        textViewTitle.setTextColor(this.titleTextColor);
        textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textTitleSize);

        textViewDesc = (TextView) viewGroupTutorContent.findViewById(R.id.text_description);
        textViewDesc.setTextColor(this.textColor);
        textViewDesc.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);

        line = viewGroupTutorContent.findViewById(R.id.view_line);
        // line2 = viewGroupTutorContent.findViewById(R.id.view_line2);
        // line3 = viewGroupTutorContent.findViewById(R.id.view_line3);
        if (line != null) {
            line.setBackgroundColor(textColor);
            //line2.setBackgroundColor(textColor);
            // line3.setBackgroundColor(textColor);
        }

        prevButton = (TextView) viewGroupTutorContent.findViewById(R.id.text_previous);
        prevButtonlin = viewGroupTutorContent.findViewById(R.id.prevButtonlin);
        nextButton = (TextView) viewGroupTutorContent.findViewById(R.id.text_next);
        nextButtonlin = viewGroupTutorContent.findViewById(R.id.nextButtonlin);

        viewGroupIndicator = (ViewGroup) viewGroupTutorContent.findViewById(R.id.view_group_indicator);

        if (prevButton != null) {
            prevButton.setText(prevString);
            prevButton.setTextColor(this.textColor);
            prevButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);
            prevButtonlin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (showCaseListener != null) {
                        if (ShowCaseLayout.this.isStart && hasSkipWord) {
                            ShowCaseLayout.this.showCaseListener.onComplete();
                            //set it's shown to true if skip click in first showcase in home fragmnet
                            ShowCasePreference.setSkipAllShown(context, true);

                        } else {
                            ShowCaseLayout.this.showCaseListener.onPrevious();
                        }
                    }
                }
            });
        }
        if (nextButton != null) {
            nextButton.setTextColor(this.textColor);
            nextButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, this.textSize);
            nextButtonlin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNextClicked();
                }
            });
        }

        this.addView(viewGroup);
        Log.d(TAG, "initContent:addView ");
    }

    private void moveViewBasedHighlight(int highlightXstart,
                                        int highlightYstart,
                                        int highlightXend,
                                        int highlightYend) {
        if (showCaseContentPosition == ShowCaseContentPosition.UNDEFINED) {
            int widthCenter = this.getWidth() / 2;
            int heightCenter = this.getHeight() / 2;
            if (highlightYend <= heightCenter) {
                showCaseContentPosition = ShowCaseContentPosition.BOTTOM;
            } else if (highlightYstart >= heightCenter) {
                showCaseContentPosition = ShowCaseContentPosition.TOP;
            } else if (highlightXend <= widthCenter) {
                showCaseContentPosition = ShowCaseContentPosition.RIGHT;
            } else if (highlightXstart >= widthCenter) {
                showCaseContentPosition = ShowCaseContentPosition.LEFT;
            } else { // not fit anywhere
                // if bottom is bigger, put to bottom, else put it on top
                if ((this.getHeight() - highlightYend) > highlightYstart) {
                    showCaseContentPosition = ShowCaseContentPosition.BOTTOM;
                } else {
                    showCaseContentPosition = ShowCaseContentPosition.TOP;
                }
            }
        }

        LayoutParams layoutParams;
        switch (showCaseContentPosition) {
            case RIGHT: {
                int expectedWidth = getWidth() - highlightXend - 2 * this.spacing;

                viewGroup.measure(MeasureSpec.makeMeasureSpec(expectedWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int viewGroupHeight = viewGroup.getMeasuredHeight();

                layoutParams = new LayoutParams(
                        expectedWidth,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.RIGHT);
                layoutParams.rightMargin = this.spacing;
                layoutParams.leftMargin = this.spacing;
                layoutParams.bottomMargin = 0;

                // calculate diff top height between object and the content;
                int hightLightHeight = (highlightYend - highlightYstart);
                int diffHeight = hightLightHeight - viewGroupHeight;

                // check top margin. top should not out of window
                int expectedTopMargin = highlightYstart + diffHeight / 2;
                checkMarginTopBottom(expectedTopMargin, layoutParams, viewGroupHeight);

                setLayoutViewGroup(layoutParams);

                if (arrowWidth == 0) {
                    path = null;
                } else {
                    int highLightCenterY = (highlightYend + highlightYstart) / 2;
                    int recalcArrowWidth = getRecalculateArrowWidth(highLightCenterY, getHeight());
                    if (recalcArrowWidth == 0) {
                        path = null;
                    } else {
                        path = new Path();
                        path.moveTo(highlightXend + this.arrowMargin, highLightCenterY);
                        path.lineTo(highlightXend + this.spacing,
                                highLightCenterY - arrowWidth / 2);
                        path.lineTo(highlightXend + this.spacing,
                                highLightCenterY + arrowWidth / 2);
                        path.close();
                    }
                }
            }
            break;
            case LEFT: {
                int expectedWidth = highlightXstart - 2 * this.spacing;

                viewGroup.measure(MeasureSpec.makeMeasureSpec(expectedWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int viewGroupHeight = viewGroup.getMeasuredHeight();

                layoutParams = new LayoutParams(
                        expectedWidth,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.LEFT);
                layoutParams.leftMargin = this.spacing;
                layoutParams.rightMargin = this.spacing;
                layoutParams.bottomMargin = 0;

                // calculate diff top height between object and the content;
                int hightLightHeight = (highlightYend - highlightYstart);
                int diffHeight = hightLightHeight - viewGroupHeight;

                // check top margin. top should not out of window
                int expectedTopMargin = highlightYstart + diffHeight / 2;
                checkMarginTopBottom(expectedTopMargin, layoutParams, viewGroupHeight);

                setLayoutViewGroup(layoutParams);

                if (arrowWidth == 0) {
                    path = null;
                } else {
                    int highLightCenterY = (highlightYend + highlightYstart) / 2;
                    int recalcArrowWidth = getRecalculateArrowWidth(highLightCenterY, getHeight());
                    if (recalcArrowWidth == 0) {
                        path = null;
                    } else {
                        path = new Path();
                        path.moveTo(highlightXstart - this.arrowMargin, highLightCenterY);
                        path.lineTo(highlightXstart - this.spacing,
                                highLightCenterY - arrowWidth / 2);
                        path.lineTo(highlightXstart - this.spacing,
                                highLightCenterY + arrowWidth / 2);
                        path.close();
                    }
                }
            }
            break;
            case BOTTOM: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.TOP);
                layoutParams.topMargin = highlightYend + this.spacing;
                layoutParams.leftMargin = this.spacing;
                layoutParams.rightMargin = this.spacing;
                layoutParams.bottomMargin = 0;

                setLayoutViewGroup(layoutParams);

                if (arrowWidth == 0) {
                    path = null;
                } else {
                    int highLightCenterX = (highlightXend + highlightXstart) / 2;
                    int recalcArrowWidth = getRecalculateArrowWidth(highLightCenterX, getWidth());
                    if (recalcArrowWidth == 0) {
                        path = null;
                    } else {
                        path = new Path();
                        path.moveTo(highLightCenterX, highlightYend + this.arrowMargin);
                        path.lineTo(highLightCenterX - recalcArrowWidth / 2,
                                highlightYend + this.spacing);
                        path.lineTo(highLightCenterX + recalcArrowWidth / 2,
                                highlightYend + this.spacing);
                        path.close();
                    }
                }
            }
            break;
            case TOP: {
                layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM);
                layoutParams.bottomMargin = getHeight() - highlightYstart + this.spacing;
                layoutParams.topMargin = 0;
                layoutParams.leftMargin = this.spacing;
                layoutParams.rightMargin = this.spacing;

                setLayoutViewGroup(layoutParams);

                if (arrowWidth == 0) {
                    path = null;
                } else {
                    int highLightCenterX = (highlightXend + highlightXstart) / 2;
                    int recalcArrowWidth = getRecalculateArrowWidth(highLightCenterX, getWidth());
                    if (recalcArrowWidth == 0) {
                        path = null;
                    } else {
                        path = new Path();
                        path.moveTo(highLightCenterX, highlightYstart - this.arrowMargin);
                        path.lineTo(highLightCenterX - recalcArrowWidth / 2,
                                highlightYstart - this.spacing);
                        path.lineTo(highLightCenterX + recalcArrowWidth / 2,
                                highlightYstart - this.spacing);
                        path.close();
                    }
                }
            }
            break;
            case UNDEFINED:
                moveViewToCenter();
                break;
        }
        // set a global layout listener which will be called when the layout pass is completed and the view is drawn
        line.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        //Remove the listener before proceeding
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            line.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            line.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }

                        final int[] location = new int[2];
                        line.getLocationInWindow(location);
                        Log.d(TAG, "onGlobalLayout: getLocationInWindow line2 " + line.getY() + " y" + line.getX() + "x " + location[0] + " " + location[1]);                        // measure your views here
                        line.getLocationOnScreen(location);
                        Log.d(TAG, "onGlobalLayout: getLocationOnScreen line2 two " + location[0] + " " + location[1]);                        // measure your views here

                        Rect rectf = new Rect();

                        line.getGlobalVisibleRect(rectf);

                        Log.d("WIDTH  line2        :", String.valueOf(rectf.width()));
                        Log.d("HEIGHT line2      :", String.valueOf(rectf.height()));
                        Log.d("left   line2      :", String.valueOf(rectf.left));
                        Log.d("right  line2      :", String.valueOf(rectf.right));
                        Log.d("top      line2    :", String.valueOf(rectf.top));
                        Log.d("bottom   line2    :", String.valueOf(rectf.bottom));
                    }
                }
        );
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        Log.d(TAG, "onWindowFocusChanged: ");
        if (goThroughWindowF) {
            goThroughWindowF = false;
            Log.d(TAG, "onWindowFocusChanged:line  " + line.getX() + "x " + line.getY() + " y");

        }
    }

    private void setLayoutViewGroup(LayoutParams params) {
        this.viewGroup.setVisibility(View.INVISIBLE);

        this.viewGroup.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ShowCaseLayout.this.viewGroup.setVisibility(View.VISIBLE);
                ShowCaseLayout.this.viewGroup.removeOnLayoutChangeListener(this);
            }
        });
        this.viewGroup.setLayoutParams(params);
        invalidate();
    }

    private int getRecalculateArrowWidth(int highlightCenter, int maxWidthOrHeight) {
        int recalcArrowWidth = arrowWidth;
        int safeArrowWidth = this.spacing + (arrowWidth / 2);
        if (highlightCenter < safeArrowWidth ||
                highlightCenter > (maxWidthOrHeight - safeArrowWidth)) {
            recalcArrowWidth = 0;
        }
        return recalcArrowWidth;
    }

    private void moveViewToCenter() {
        showCaseContentPosition = ShowCaseContentPosition.UNDEFINED;

        LayoutParams layoutParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        layoutParams.rightMargin = this.spacing;
        layoutParams.leftMargin = this.spacing;
        layoutParams.bottomMargin = this.spacing;
        layoutParams.topMargin = this.spacing;

        setLayoutViewGroup(layoutParams);
        this.path = null;
    }

    private void checkMarginTopBottom(int expectedTopMargin,
                                      LayoutParams layoutParams,
                                      int viewHeight) {
        if (expectedTopMargin < this.spacing) {
            layoutParams.topMargin = this.spacing;
        } else {
            // check bottom margin. bottom should not out of window
            int prevActualHeight = expectedTopMargin + viewHeight + this.spacing;
            if (prevActualHeight > getHeight()) {
                int diff = prevActualHeight - getHeight();
                layoutParams.topMargin = expectedTopMargin - diff;
            } else {
                layoutParams.topMargin = expectedTopMargin;
            }
        }
    }

    private void recycleResources() {
        if (this.bitmap != null) {
            this.bitmap.recycle();
        }
        this.bitmap = null;
        if (this.lastTutorialView != null) {
            this.lastTutorialView.setDrawingCacheEnabled(false);
        }
        this.lastTutorialView = null;
        this.viewPaint = null;
    }

    private Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }
}