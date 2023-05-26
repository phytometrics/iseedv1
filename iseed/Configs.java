package jp.phytometrics.iseed;

import java.util.HashMap;
import java.util.Map;

public class Configs {
    public static Map<String, String> nModuleMap = new HashMap<String, String>(){
        {
            put("fast1280", "best.torchscript_1280_s.pt");
            put("faster1280", "best.torchscript_gpu.pt");
            put("normal1280", "best.torchscript_1280_m.pt");
            put("accurate1280", "best.torchscript_1280_l.pt");
        }
    };

    public static String mModuleType = "fast1280";
    public static String mModuleName;

    // for yolov5 model, no need to apply MEAN and STD
    public static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    public static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};

    public static int mInputWidth;
    public static int mInputHeight;

    public static int mOutputRow;
    public static int mOutputColumn;

    public static float mThreshold = 0.5f; // score above which a detection is generated nms threshold.

    public static int mNmsLimit = 2000;


    public static void main(){
        mModuleName =  nModuleMap.get(mModuleType);
        if (mModuleType.contains("1280"))
        {
            //System.out.println("1280 mode");
            //System.out.println(mModuleName);
            // model input image size
            mInputWidth = 1280;
            mInputHeight = 1280;
            // model output is of size 102000*6
            mOutputRow = 102000;
            mOutputColumn = 6; // left, top, right, bottom, score and 1 class probability
        }
        else{
            //System.out.println("640 mode");
            // model input image size
            mInputWidth = 640;
            mInputHeight = 640;
            // model output is of size 25200*6
            mOutputRow = 25200;
            mOutputColumn = 6; // left, top, right, bottom, score and 1 class probability

        }
    }

    public static void updateModel(){

    }



}
