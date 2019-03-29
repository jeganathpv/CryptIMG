package com.jaddu.cryptimage;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;


public class DecryptActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    ImageView imageView;
    Button  browseButton;
    TextView textView;
    Button decryptButton;
    Intent gallery;
    EditText passwd;
    private static final int PICK_IMAGE=100;
    public static final int TOP_MARGIN = 13;
    public static  int IMAGE_ADD=0;
    public static final int SPACE = 14;
//    public static int PASS_MATCHED=0;
    Uri imageUri;
    String filename;
    Bitmap originalImage;
    Bitmap decryptImage;

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt);
        imageView=findViewById(R.id.imageView);
        browseButton=findViewById(R.id.browseButton);
        passwd=findViewById(R.id.passwdText);
        textView=  findViewById(R.id.textView);
        decryptButton= findViewById(R.id.decryptButton);

        mAuth= FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()==null){
                    startActivity(new Intent(DecryptActivity.this,LoginActivity.class));
                }
            }
        };


        //getting bottom navigation view and attaching the listener
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(this);


        //this button is used to browse files from gallery
        browseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(IMAGE_ADD==1){
                    decryptImage();
                }else {
                    Toast.makeText(DecryptActivity.this,"Please add picture before encrypt a text",Toast.LENGTH_SHORT).show();
                }

            }
        });

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
            Toast.makeText(DecryptActivity.this,"You have been logged out!",Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            startActivity(new Intent(getBaseContext(),SplashActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){
            case R.id.encrypt:{
                Intent intent=new Intent(getBaseContext(), MainActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.decrypt:{
                return true;
            }
        }

        return true;
    }


    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(DecryptActivity.this);
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

    private void decryptImage() {
        InputStream fis= null;
        try {
            fis = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        originalImage= BitmapFactory.decodeStream(fis);
        decryptImage=originalImage.copy(originalImage.getConfig(),true);

        int width=decryptImage.getWidth();
        int height=decryptImage.getHeight()-TOP_MARGIN;


        ArrayList<Character> characters = new ArrayList<>();
        ArrayList<Character> authtext=new ArrayList<>();


        for (int pixel = 0; pixel < width; pixel++) {
            if (pixel % SPACE == 0) {
                int color=decryptImage.getPixel(pixel,TOP_MARGIN);
                int greenhex= Color.green(color);
                characters.add((char)greenhex);
            }
        }

        StringBuilder text =new StringBuilder();
        for (char character : characters)
            text.append(character);

        String decrptedText=text.toString();

        StringBuilder finalText=new StringBuilder();
        String textLen=decrptedText.substring(0, 2);
        int textLength=Integer.parseInt(textLen);
        for(int i=2;i<=textLength+1;i++)
            finalText.append(decrptedText.charAt(i));

        decrptedText=finalText.toString();
        decrptedText=decryptText(decrptedText);

        //Decrypt Auth Details
        for (int pixel = 0; pixel < width; pixel++) {
            if (pixel % SPACE == 0) {
                int color=decryptImage.getPixel(pixel,height);
                int greenhex= Color.green(color);
                authtext.add((char)greenhex);
            }
        }

        StringBuilder auth=new StringBuilder();
        for(char c:authtext)
            auth.append(c);

        String decryptedAuth=auth.toString();

        StringBuilder finalAuth=new StringBuilder();
        textLen=decryptedAuth.substring(0, 2);
        textLength=Integer.parseInt(textLen);
        for(int i=2;i<=textLength+1;i++)
            finalAuth.append(decryptedAuth.charAt(i));

        decryptedAuth=finalAuth.toString();

        String[] auths=decryptedAuth.split(" ");


        String currentUser= Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();
        assert currentUser != null;
        currentUser=currentUser.replace("@gmail.com","");
        String passwdText=passwd.getText().toString();
//        final String authEncrypt=currentUser+" "+passwdText;

        if(auths[0].equals(currentUser)){
            if(auths[1].equals(passwdText)){
                textView.setText(decrptedText);
            }else {
                Toast.makeText(DecryptActivity.this,"Password Mismatch",Toast.LENGTH_SHORT).show();
            }

        }else{
            Toast.makeText(DecryptActivity.this,"User Mismatch",Toast.LENGTH_SHORT).show();
        }


    }

    //Cipher Decryption
    public static String decryptText(String text) {

        int textLen=text.length();
        //Change Characters in even to position
        char[] textChar=text.toCharArray();

        char temp=textChar[0];
        textChar[0]=textChar[textLen-1];
        textChar[textLen-1]=temp;

        for(int i=0;i<text.length();i++) {
            if(i%2==0) {
                textChar[i]--;
            }
        }
        text=new String(textChar);


        //Split string into two substring
        String textA="",textB="";
        for(int i=0;i<(textLen/2);i++) {
            textA+=text.charAt(i);
        }
        for(int i=(textLen/2);i<text.length();i++) {
            textB+=text.charAt(i);
        }
        //Merge two substrings
        text=textB+textA;
        //Reversing the string
        String reverse = "";
        for(int i = text.length() - 1; i >= 0; i--)
        {
            reverse = reverse + text.charAt(i);
        }
        text=reverse;
        //Removing # from the string
        if(text.length()%2==0) {
            if(text.contains("#")) {
                text=text.replace("#","");
            }
        }

        return text;
    }




    //Open Gallery to pick image
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

            IMAGE_ADD=1;

        }
    }

}
