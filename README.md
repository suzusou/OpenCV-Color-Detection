<p align="center">
  <a href="https://opencv.org/" rel="noopener" target="_blank"><img width="150" src="/OpenCV_logo.svg" alt="OpenCV logo"></a>
</p>

<h1 align="center">OpenCV-Color-Detection</h1>

This program uses `OpenCV` to process `CameraX information`.   
## Setting
``` Java
imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(176, 144))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
``` 
At setTargetResolution(new Size(x, y)), set the resolution. 
``` Java
    private class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private Mat matPrevious = null;

        @Override
        public void analyze(@NonNull ImageProxy image) {

            Mat matOrg = getMatFromImage(image);

            Mat mat = fixMatRotation(matOrg);

            if (matPrevious == null) matPrevious = mat;
            matPrevious = mat;

            Bitmap bitmap = Bitmap.createBitmap(matPrevious.cols(), matPrevious.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(matPrevious, bitmap);
            
            //Describe the process you want to process in this class

            image.close();
        }
``` 
In this case, if you continue to take the average of the `RGB`, you can see it in the log.　　

## Difference
Processing speed has been increased by cutting the library files by about `one-third`.

## Finally
For more information, tap the logo above and visit the official website.  
If you found this helpful, please give us a star.
