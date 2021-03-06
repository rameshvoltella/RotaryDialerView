package com.vishnus1224.rotarydialer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

public class RotaryDialer extends View implements View.OnTouchListener {

    private static final int TOTAL_NUMBERS_ON_THE_DIALER = 10;

    private DialerNumber dialerNumbers[] = new DialerNumber[TOTAL_NUMBERS_ON_THE_DIALER];

    private DialerCover dialerCover;

    private Lever lever;

    private Paint backgroundPaint;

    private boolean touchInProgress = false;

    private float startX = 0f;
    private float startY = 0f;

    private float lastEndX = 0f;
    private float lastEndY = 0f;

    /**
     * Width of the view.
     */
    private int dialerWidth;

    /**
     * Height of the dialer.
     */
    private int dialerHeight;

    /**
     * Radius of the view.
     */
    private float dialerRadius;

    /**
     * The number that the user has currently touched.
     */
    private int touchedNumber;

    /**
     * The radius of the circle from the center to the topmost portion of the numbers.
     * Used for calculating whether the touch point lies between the outer and inner circles.
     */
    private float innerRadius;

    /**
     * To animator the dialer back to it's original position.
     */
    private ValueAnimator dialerAnimator = ValueAnimator.ofFloat(0f);

    /**
     * The duration of the animation to reset the dialer position.
     */
    private long animationDuration = 1000L;

    /**
     * Flag to indicate that reset animation is currently in progress and prevent touch event during this time.
     */
    private boolean animationInProgress = false;

    public RotaryDialer(Context context) {
        super(context);
        initPaints();
        initNumbers();
        initCover();
        initLever();
        initAnimator();
        setClickable(true);
        setOnTouchListener(this);
    }

    public RotaryDialer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotaryDialer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
        initNumbers();
        initCover();
        initLever();
        initAnimator();
        setClickable(true);
        setOnTouchListener(this);
    }

    private void initPaints() {

        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.RED);
    }

    private void initNumbers(){

        for(int i = 0; i < TOTAL_NUMBERS_ON_THE_DIALER; i++){
            DialerNumber dialerNumber = new DialerNumber(getContext());
            dialerNumber.setPosition(i);
            dialerNumbers[i] = dialerNumber;
        }

    }

    private void initCover() {
        dialerCover = new DialerCover(getContext());
        dialerCover.setDialerNumbers(dialerNumbers);

        DialerNumber dialerNumber = new DialerNumber(getContext());
        dialerNumber.setCenter(new Point(0, 0));
        dialerNumber.setCircleRadius(20f);

        boolean value = Util.pointLiesInCircle(19, 19, dialerNumber);
        Log.d("point in the circle", "is " + value);
    }

    private void initLever(){
        lever = new Lever(getContext());
    }


    private void initAnimator() {
        dialerAnimator.setInterpolator(new DecelerateInterpolator());
        dialerAnimator.setDuration(animationDuration);
        dialerAnimator.addUpdateListener(animatorUpdateListener);

        dialerAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationInProgress = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                animationInProgress = true;
            }
        });
    }

    private ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {

            dialerCover.angle = (float) valueAnimator.getAnimatedValue();
            invalidate();
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {

        dialerWidth = getWidth();
        dialerHeight = getHeight();

        //set the radius to the minimum dimension.
        dialerRadius = dialerWidth > dialerHeight ? dialerHeight * 0.5f : dialerWidth * 0.5f;

        canvas.drawCircle(dialerWidth * 0.5f, dialerHeight * 0.5f, dialerRadius, backgroundPaint);

        drawNumbers(canvas);

        drawCover(canvas);

        drawLever(canvas);

    }

    private void drawNumbers(Canvas canvas) {

        float angle = 90f;

        // TODO: 12/8/2015 change 270 and use Math.PI class.
        float stepSize = (270f / TOTAL_NUMBERS_ON_THE_DIALER);

        int minimumDimension = Math.min(dialerWidth, dialerHeight);

        float distanceFromCentre = minimumDimension / 2.5f;

        innerRadius = distanceFromCentre - (minimumDimension / 12f);

        for (int i = 0; i < TOTAL_NUMBERS_ON_THE_DIALER; i++) {

            DialerNumber dialerNumber = dialerNumbers[i];

            int x = (int) (dialerWidth * 0.5f + (distanceFromCentre * Math.cos(Math.toRadians(angle))));
            int y = (int) (dialerHeight * 0.5f + (distanceFromCentre * Math.sin(Math.toRadians(angle))));

            dialerNumber.setCenter(new Point(x, y));
            dialerNumber.setCircleRadius(minimumDimension / 12);
            dialerNumber.draw(canvas);

            angle += stepSize;
        }

    }

    private void drawCover(Canvas canvas) {

        dialerCover.setCenter(new Point(dialerWidth / 2, dialerHeight / 2));
        dialerCover.setCoverWidth(dialerWidth);
        dialerCover.setCoverHeight(dialerHeight);
        dialerCover.setRadius(dialerRadius);
        dialerCover.draw(canvas);

    }


    private void drawLever(Canvas canvas) {

        lever.setCenterX(dialerWidth * 0.5f);
        lever.setCenterY(dialerHeight * 0.5f);
        lever.setDialerRadius(dialerRadius);
        lever.draw(canvas);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:

                //if the reset animation is not currently taking place, then process the touch events.
                if(!animationInProgress) {

                    for (int i = 0; i < TOTAL_NUMBERS_ON_THE_DIALER; i++) {
                        if (Util.pointLiesInCircle(motionEvent.getX(), motionEvent.getY(), dialerNumbers[i])) {

                            startX = motionEvent.getX();
                            startY = motionEvent.getY();

                            lastEndX = startX;
                            lastEndY = startY;

                            touchedNumber = i;

                            touchInProgress = true;
                            return true;
                        }
                    }

                }

            case MotionEvent.ACTION_MOVE:

                if(touchInProgress){

                    //get the latest touch position.
                    final float endX = motionEvent.getX();
                    final float endY = motionEvent.getY();

                    //user can only rotate the dialer when the finger moves along the region containing the numbers.
                    //if the point is outside the valid touch region, then do not process further.
                    if(!Util.pointLiesBetweenCircles(endX, endY, dialerWidth * 0.5f, dialerHeight * 0.5f, innerRadius, dialerRadius)){
                        resetDialerPosition();
                        return true;
                    }

                    if(lever.getLocationRect().contains((int)endX, (int)endY)){
                        //the lever has been touched, so reset to the initial position.
                        Toast.makeText(getContext(), "" + touchedNumber, Toast.LENGTH_SHORT).show();
                        resetDialerPosition();
                        return true;
                    }

                    //calculate the rotation angle between starting and ending touch position.
                    final double rotationAngle = Util.angleBetweenPointsInDegrees(startX, startY, endX, endY, dialerWidth * 0.5f, dialerHeight * 0.5f);

                    final double lastAngle = Util.angleBetweenPointsInDegrees(lastEndX, lastEndY, endX, endY, dialerWidth * 0.5f, dialerHeight * 0.5f);

                    lastEndX = endX;
                    lastEndY = endY;

                    if(lastAngle < 0 || lastAngle > 350){

                        if(lastAngle < -300){
                            break;
                        }

                        resetDialerPosition();

                        return true;
                    }

                    dialerCover.angle = rotationAngle;
                    invalidate();


                }

                return true;

            case MotionEvent.ACTION_UP:

                if(touchInProgress) {
                    resetDialerPosition();
                }

                break;
            case MotionEvent.ACTION_CANCEL:

                if(touchInProgress) {
                    resetDialerPosition();
                }

                break;
        }

        return super.onTouchEvent(motionEvent);
    }

    private void resetDialerPosition(){

        touchInProgress = false;

        float initialAngle = (float) (dialerCover.angle >= 0 ? dialerCover.angle : 360f + dialerCover.angle);

        Log.d("angle", "" + dialerCover.angle);

        dialerAnimator.setFloatValues(initialAngle, 0f);
        dialerAnimator.start();

    }

}