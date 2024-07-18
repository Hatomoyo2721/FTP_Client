//package com.example.ftp_client.ui.file.file_http;
//
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.AsyncTask;
//import android.util.Log;
//import android.webkit.MimeTypeMap;
//import android.widget.Toast;
//
//import androidx.core.content.FileProvider;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//import okhttp3.ResponseBody;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class FileDownloadService {
//
//    private final Context context;
//
//    public FileDownloadService(Context context) {
//        this.context = context;
//    }
//
//    public void downloadFile(String fileUrl) {
//        new DownloadFileTask().execute(fileUrl);
//    }
//
//    private class DownloadFileTask extends AsyncTask<String, Void, File> {
//        @Override
//        protected File doInBackground(String... strings) {
//            String fileUrl = strings[0];
//
//            try {
//                String baseUrl = fileUrl.substring(0, fileUrl.lastIndexOf("/") + 1);
//                String filePath = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
//
//                FileDownloadApi downloadApi = createDownloadApi(baseUrl);
//                retrofit2.Response<ResponseBody> response = downloadApi.downloadFile(filePath).execute();
//
//                if (response.isSuccessful()) {
//                    return saveFile(response.body());
//                }
//            } catch (IOException e) {
//                Log.e("FileDownloadService", "Error downloading file: " + e.getMessage());
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(File file) {
//            if (file != null) {
//                openFile(file);
//            } else {
//                Toast.makeText(context, "Failed to download file", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        private File saveFile(ResponseBody body) throws IOException {
//            File file = new File(context.getCacheDir(), "downloadedFile");
//            try (InputStream inputStream = body.byteStream();
//                 FileOutputStream outputStream = new FileOutputStream(file)) {
//                byte[] buffer = new byte[4096];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    outputStream.write(buffer, 0, bytesRead);
//                }
//            }
//            return file;
//        }
//
//        private void openFile(File file) {
//            MimeTypeMap mime = MimeTypeMap.getSingleton();
//            String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
//            String type = mime.getMimeTypeFromExtension(ext);
//
//            Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
//
//            Intent intent = new Intent(Intent.ACTION_VIEW);
//            intent.setDataAndType(fileUri, type);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            context.startActivity(intent);
//        }
//
//        private FileDownloadApi createDownloadApi(String baseUrl) {
//            return new Retrofit.Builder()
//                    .baseUrl(baseUrl)
//                    .addConverterFactory(GsonConverterFactory.create())
//                    .build()
//                    .create(FileDownloadApi.class);
//        }
//    }
//}
