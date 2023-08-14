#### [HIRE US](http://vrgsoft.net/)
# VideoCrop
Video cropping library with trimming and opportunity to choose different aspect ratio types</br></br>
<img src="https://github.com/VRGsoftUA/VideoCrop/blob/master/video.gif" width="270" height="480" />
# Usage
*For a working implementation, Have a look at the Sample Project - app*
1. Include the library as local library project.
```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
dependencies {
    implementation 'com.github.VRGsoftUA:VideoCrop:1.0'
}
```
2. In code you need to start Activityfor result like so:
```
startActivityForResult(VideoCropActivity.createIntent(this, inputPath, outputPath), CROP_REQUEST);
```
3. Then catch result in onActivityResult callback
```@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CROP_REQUEST && resultCode == RESULT_OK){
            //crop successful
        }
    }
```
#### Contributing
* Contributions are always welcome
* If you want a feature and can code, feel free to fork and add the change yourself and make a pull request

#### Produce a bundle file
* Push the changes you want to implement in the new bundle. Preferably use the desired bundle name as the commit message.
* Create a tag using this commit and push it following this guide: https://linuxhint.com/push-git-tags-to-remote-repository/#:~:text=To%20push%20Git%20tags%20to%20the%20remote%20repository%2C%20first%2C%20open,tag%2Dname%3E%E2%80%9D%20command
* This website: https://jitpack.io/#MinlyInc/VideoCrop can be used to search for and view the different versions of the bundle.