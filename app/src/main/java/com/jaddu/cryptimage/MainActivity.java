package com.jaddu.cryptimage;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{

    ImageView imageView;
    Button button;
    EditText editText;
    EditText mailText;
    EditText passwd;
    Button encryptButton;
    private static final int PICK_IMAGE=100;
    private static final int PERMISSION_REQUEST_CODE = 200;
    public static final int TOP_MARGIN = 13;
    public static final int SPACE = 14;
    public static int ENCRYPT_IMAGE =0;
    public static int IMAGE_ADD=0;
    Uri imageUri;
    String filename;
    String mailid;
    String password;
    String textMsg;
    //    Bitmap bitmap;
    File dir;
    File filePath;
    Intent gallery;
    OutputStream outputStream;
    Bitmap originalImage;
    Bitmap encryptImage;

    //Firebase attributes
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button=findViewById(R.id.button);
        imageView=findViewById(R.id.imageView);
        editText=findViewById(R.id.editText);
        mailText=findViewById(R.id.mailText);
        passwd=findViewById(R.id.passwdText);
        encryptButton=findViewById(R.id.encryptButton);

        //getting bottom navigation view and attaching the listener
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(this);

        //firebase auth listener
        mAuth=FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()==null){
                    startActivity(new Intent(MainActivity.this,LoginActivity.class));
                }
            }
        };

        //this button is used to browse files from gallery
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        //encryption process is done in this area
        encryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IMAGE_ADD==1){
                    mailid=mailText.getText().toString();
                    password=passwd.getText().toString();
                    textMsg=editText.getText().toString();
                    if(!mailid.equals("")){
                        if(!password.equals("")){
                            if(!textMsg.equals("")){
                                encrptImage();
                            }else{
                                Toast.makeText(MainActivity.this,"Enter the text to encrypt",Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(MainActivity.this,"Please create secret key to encrypt",Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(MainActivity.this,"Enter the sender email address",Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(MainActivity.this,"Please add picture before encrypt a text",Toast.LENGTH_SHORT).show();
                }

            }
        });


    }


    private void savePicture() {
        //here we create bitmap image
//        bitmap=BitmapFactory.decodeResource(getResources(),R.drawable.jaddupatern);

        //this provide dir-- storage/emulated/0/
        filePath= Environment.getExternalStorageDirectory();
        //we create directory for CryptIMG
        dir=new File(filePath.getAbsolutePath()+"/CryptIMG");
//        dir.mkdirs();
        boolean isDirectoryCreated= dir.exists();
        if (!isDirectoryCreated) dir.mkdirs();


        //it returns file name for new file using date format
        String pictureName=getPictureName();
        File file=new File(dir,pictureName);
        try{
            outputStream=new FileOutputStream(file);
            encryptImage.compress(Bitmap.CompressFormat.PNG,100,outputStream);
//            bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(MainActivity.this,"Image saved"+file.toString(), Toast.LENGTH_SHORT).show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private String getPictureName() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd_HHmm");
        String timestamp=sdf.format(new Date());
        return "CryptIMG"+timestamp+".png";
    }

    private void encrptImage() {
        int index = 0;

        ArrayList<Character> chars = new ArrayList<>();
        ArrayList<Integer> asciiChars = new ArrayList<>();
        ArrayList<Character> auths = new ArrayList<>();
        ArrayList<Integer> asciiAuths = new ArrayList<>();


        InputStream fis= null;
        try {
            fis = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        originalImage= BitmapFactory.decodeStream(fis);

        encryptImage=originalImage.copy(originalImage.getConfig(),true);
        String currentUser= Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();

        String text=textMsg;
        text=encryptText(text);


        Map<String,String> taskMap = new HashMap<>();
        taskMap.put("ReceiverID", mailid);
        taskMap.put("Password",password);
        taskMap.put("Text",text);
        taskMap.put("Sender ID",currentUser);

        FirebaseDatabase.getInstance().getReference().push().setValue(taskMap);
        mailid=mailid.replace("@gmail.com","");



        int width=encryptImage.getWidth();
//        String text=textMsg;
//        text=encryptText(text);
        if(text.length()<10)
            text= "0"+text.length() + text;
        else
            text= text.length() + text;

        for(char c: text.toCharArray())
            chars.add(c);

        for(char c:chars)
            asciiChars.add((int)c);

        for (int x = 0; x < width; x++) {
            if (x % SPACE == 0 && index < asciiChars.size()) {
                encryptImage.setPixel(x,TOP_MARGIN, Color.rgb(1,asciiChars.get(index),1));
                index++;
            }
        }
        index=0;

        text=mailid+" "+password;
        int height=encryptImage.getHeight()-TOP_MARGIN;


        if(text.length()<10)
            text= "0"+text.length() + text;
        else
            text= text.length() + text;

        for(char c: text.toCharArray())
            auths.add(c);

        for(char c:auths)
            asciiAuths.add((int)c);

        for (int x = 0; x < width; x++) {
            if (x % SPACE == 0 && index < asciiAuths.size()) {
                encryptImage.setPixel(x,height, Color.rgb(1,asciiAuths.get(index),1));
                index++;
            }
        }


        ENCRYPT_IMAGE=1;
        Toast.makeText(MainActivity.this,R.string.toast_encryption,Toast.LENGTH_LONG).show();
        afterEncryption();


    }

    public static String encryptText(String text) {
        //Adding # to the string
        if(text.length()%2!=0) {
            text+="#";
        }
        //Reversing the string
        String reverse;
        reverse = "";
        for(int i = text.length() - 1; i >= 0; i--)
            reverse = reverse + text.charAt(i);
        text=reverse;
        //Split string into two substring
        int textLen=text.length();
        String textA="",textB="";
        for(int i=0;i<(textLen/2);i++) {
            textA+=text.charAt(i);
        }
        for(int i=(textLen/2);i<text.length();i++) {
            textB+=text.charAt(i);
        }
        //Merge two substrings
        text=textB+textA;

        //Change Characters in even to position
        char[] textChar=text.toCharArray();
        for(int i=0;i<text.length();i++) {
            if(i%2==0) {
                textChar[i]++;
            }
        }
        //Change First and Last Char
        char temp=textChar[0];
        textChar[0]=textChar[textLen-1];
        textChar[textLen-1]=temp;

        //Make Char array to string
        text=new String(textChar);

        return text;
    }

    private void afterEncryption(){
        AlertDialog.Builder builder= new AlertDialog.Builder(this);
        builder.setTitle("Save Picture")
                .setMessage("Click OK to save image in Gallery.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this,"Please wait, it may take some time",Toast.LENGTH_LONG).show();
                        //after marshmallow android has protection so we need to provide runtime permission
                        if (!checkPermission()) {
                            savePicture();
                        } else {
                            if (checkPermission()) {
                                requestPermissionAndContinue();
                            } else {
                                savePicture();
                            }
                        }

                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void openGallery(){
        gallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery,PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestcode,int resultcode,Intent data){
        super.onActivityResult(requestcode,resultcode,data);
        if(requestcode==PICK_IMAGE && resultcode==RESULT_OK){
            imageUri=data.getData();
            imageView.setImageURI(imageUri);
            filename=imageUri.getPath();
//            editText.setText(filename);
            IMAGE_ADD=1;
        }
    }


    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ;
    }

    //Below codes are user to provide runtime permission
    private void requestPermissionAndContinue() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("CryptIMG");
                alertBuilder.setMessage("Access required");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "Permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        } else {
            savePicture();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults.length > 0) {

                boolean flag = true;
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        flag = false;
                    }
                }
                if (flag) {
                    savePicture();
                } else {
                    finish();
                }

            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.about){
            AlertDialog.Builder builder= new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.app_name))
                    .setMessage(getString(R.string.about_text))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }
        if(item.getItemId()==R.id.help){
            Intent intent=new Intent(getBaseContext(), HelpActivity.class);
            startActivity(intent);

        }
        if(item.getItemId()==R.id.feedback){
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(
                            "https://api.whatsapp.com/send?phone=+919677898524"
                    )));
        }

        if(item.getItemId()==R.id.signout){
            Toast.makeText(MainActivity.this,"You have been logged out!",Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            startActivity(new Intent(getBaseContext(),SplashActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.encrypt:{
                return true;
            }
            case R.id.decrypt:{
                Intent intent=new Intent(getBaseContext(), DecryptActivity.class);
                startActivity(intent);
                return true;
            }
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure want to exit?");
        builder.setCancelable(true);
        builder.setNegativeButton("Nope", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builder.setPositiveButton("Yeah", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                finishAffinity();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
