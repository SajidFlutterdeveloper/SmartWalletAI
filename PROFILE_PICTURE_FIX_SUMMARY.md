### Profile Picture Storage Update:
The app now saves profile pictures **locally** on the mobile device instead of uploading them to Firebase Storage.

### How it works:
1.  **Image Selection**: User picks an image from the gallery.
2.  **Local Save**: The selected image is copied to the app's internal storage: `/data/user/0/com.smartwallet.ai.ui/files/profile_pics/profile_picture.jpg`.
3.  **Path Storage**: The local file path is saved in `SharedPreferences` via `PreferenceManager`.
4.  **Loading**: `Glide` loads the image using the local file path.

### Benefits:
- Works offline.
- No Firebase Storage configuration or rules required.
- Faster loading and no data usage for profile pictures.

### Related Files:
- `ProfileFragment.kt`: Updated `saveProfileImageLocally` and removed Firebase Storage code.
- `ProfileActivity.kt`: Updated `saveProfileImageLocally` and removed Firebase Storage code.
- `PreferenceManager.kt`: Stores the local path in `profile_pic_url`.
