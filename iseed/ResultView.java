// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package jp.phytometrics.iseed;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import jp.phytometrics.iseed.tflite.Classifier;

import static jp.phytometrics.iseed.MainActivity.MINIMUM_CONFIDENCE_TF_OD_API;


public class ResultView extends View {

    private final static int TEXT_X = 40;
    private final static int TEXT_Y = 35;
    private final static int TEXT_WIDTH = 260;
    private final static int TEXT_HEIGHT = 50;

    private Paint mPaintRectangle;
    private Paint mPaintText;
    private Paint mPaintCircle;
    //private Paint mPaintCircle2;
    private List<Classifier.Recognition> mResults;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        //mPaintRectangle = new Paint();
        //mPaintRectangle.setColor(Color.YELLOW);
        //mPaintText = new Paint();
        mPaintCircle = new Paint();
        //mPaintCircle2 = new Paint();
        mPaintCircle.setColor(Color.MAGENTA);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mResults == null) return;

//        mPaintCircle2.setColor(Color.BLUE);
//        canvas.drawCircle(0,0, 10.0f, mPaintCircle2);
//        canvas.drawCircle(10,10, 10.0f, mPaintCircle2);
//        canvas.drawCircle(100,100, 10.0f, mPaintCircle2);
//        canvas.drawCircle(1000,1000, 10.0f, mPaintCircle2);
        for (Classifier.Recognition result : mResults) {
            //mPaintRectangle.setStrokeWidth(5);
            //mPaintRectangle.setStyle(Paint.Style.STROKE);
            //canvas.drawRect(result.rect, mPaintRectangle);

            //Path mPath = new Path();
            //RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
            //mPath.addRect(mRectF, Path.Direction.CW);
            //mPaintText.setColor(Color.MAGENTA);
            //canvas.drawPath(mPath, mPaintText);
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                //canvas.drawRect(location, mPaintCircle);
                float centerx = (location.left + location.right) / 2;
                float centery = (location.top + location.bottom) / 2;

                canvas.drawCircle(centerx, centery, 5.3f, mPaintCircle);
                //System.out.println(centery);
            }

            //mPaintText.setColor(Color.WHITE);
            //mPaintText.setStrokeWidth(0);
            //mPaintText.setStyle(Paint.Style.FILL);
            //mPaintText.setTextSize(32);
            //canvas.drawText(String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
        }
    }

//    public void setResults(ArrayList<Result> results) {
//        mResults = results;
//    }
    public void setResults(List<Classifier.Recognition> results) {
        mResults = results;
    }
}
