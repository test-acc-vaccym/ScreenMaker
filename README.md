# The application makes screens from the front camera, encrypts it and stores encrypted images
on the local storage.

For encryption the Android Key Storage was used.
Unfortunately, Key Storage does not suits well for encrypting huge byte arrays because it works terribly slow.
That is why, the image byte array was encrypted by String Key. Than, given String key was encrypted,
using Android Key Store.


For image encryption two stage approach was used:
1. Firstly, image byte array was encrypted by the key and alias with the help of SHA1 and encrypted
array was inserted to the application local storage.
2. Secondly, the key and alias for image encryption was encrypted using Android Key Storage.



Future Plans:

1. Split openCamera method to separate modules.
2. Resize image allocation on the screen while image capturing.
3. Implement logic for supporting devices from Android 5.0 and later.
