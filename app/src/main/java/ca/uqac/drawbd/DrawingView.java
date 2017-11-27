package ca.uqac.drawbd;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

public class DrawingView extends View
{
    private LinkedList<Bitmap> canvasList = new LinkedList<>();
    private int index = 0;
    private int paintColor;
    private int alpha = 255;
    private float brushSize;
    private boolean line = false;
    private boolean erase = false;

    private float lx;
    private float ly;

    private Path drawPath;
    private Paint drawPaint, canvasPaint;
    private Canvas drawing;

    private boolean running;

    public DrawingView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        brushSize = getResources().getInteger(R.integer.medium_size);
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(brushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        clear(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        if (!running && index > 0) {
            canvasPaint.setAlpha(100);
            canvas.drawBitmap(canvasList.get(index-1), 0, 0, canvasPaint);
        }
        canvasPaint.setAlpha(255);
        canvas.drawBitmap(canvasList.get(index), 0, 0, canvasPaint);
        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (!running) {
            float touchX = event.getX();
            float touchY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    drawPath.moveTo(touchX, touchY);
                    lx = touchX;
                    ly = touchY;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (line) {
                        drawPath.reset();
                        drawPath.moveTo(lx, ly);
                    }
                    drawPath.lineTo(touchX, touchY);
                    break;
                case MotionEvent.ACTION_UP:
                    if (line) {
                        drawPath.reset();
                        drawPath.moveTo(lx, ly);
                    }
                    drawPath.lineTo(touchX, touchY);
                    drawing.drawPath(drawPath, drawPaint);
                    drawPath.reset();
                    break;
                default:
                    return false;
            }

            invalidate();
        }

        return true;
    }

    public void setColor(String newColor)
    {
        invalidate();

        if (newColor.startsWith("#")) {
            paintColor = Color.parseColor(newColor);
            drawPaint.setColor(paintColor);
            drawPaint.setAlpha(alpha);
            drawPaint.setShader(null);
        } else {
            int id = getResources().getIdentifier(newColor, "drawable", "ca.uqac.drawbd");
            Bitmap pattern = BitmapFactory.decodeResource(getResources(), id);
            BitmapShader shader = new BitmapShader(pattern, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
            drawPaint.setColor(0xFFFFFFFF);
            drawPaint.setShader(shader);
            drawPaint.setAlpha(alpha);
        }
    }

    public void setBrushSize(float newSize)
    {
        brushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, getResources().getDisplayMetrics());
        drawPaint.setStrokeWidth(brushSize);
    }

    public int getPaintAlpha()
    {
        return Math.round((float) alpha / 255 * 100);
    }

    public void setPaintAlpha(int newAlpha)
    {
        alpha = Math.round((float) newAlpha / 100 * 255);
        drawPaint.setColor(paintColor);
        drawPaint.setAlpha(alpha);
    }

    public boolean toggleLineMode()
    {
        line = !line;
        return line;
    }

    public boolean erase()
    {
        erase = !erase;

        if (erase) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            drawPaint.setXfermode(null);
        }

        return erase;
    }

    public int prev()
    {
        if (index > 0) {
            index--;
            drawing = new Canvas(canvasList.get(index));
        }
        invalidate();
        return index;
    }

    public int next()
    {
        if (++index == canvasList.size()) {
            canvasList.add(Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888));
        }
        drawing = new Canvas(canvasList.get(index));
        invalidate();
        return index;
    }

    public int copy()
    {
        Bitmap bmp = canvasList.get(index++);
        canvasList.add(index, bmp.copy(bmp.getConfig(), true));
        drawing = new Canvas(canvasList.get(index));
        return index;
    }

    public int delete()
    {
        canvasList.remove(index);
        if (index == canvasList.size()) {
            index--;
        }
        if (index < 0) {
            canvasList.add(Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888));
            index++;
        }
        drawing = new Canvas(canvasList.get(index));
        invalidate();
        return index;
    }

    public void clear()
    {
        clear(getWidth(), getHeight());
    }

    public void clear(int w, int h)
    {
        index = 0;
        canvasList.clear();
        canvasList.add(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888));
        drawing = new Canvas(canvasList.getFirst());
        invalidate();
    }

    public int frame()
    {
        return index;
    }

    public boolean startAnimationFrom(int from)
    {
        if (from < canvasList.size()) {
            index = from;
        }

        invalidate();

        running = true;

        return index + 1 < canvasList.size();
    }

    public void stop()
    {
        running = false;
    }

    public boolean nextFrame()
    {
        index++;

        invalidate();

        return index + 1 < canvasList.size();
    }
}
