package com.ahmetesadtop.example;
import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    List<String> contactsForAdapter =new ArrayList<String>();
    List<String> numbersForAdapter =new ArrayList<String>();
    List<String> internalContacts =new ArrayList<String>();
    List<String> internalNumbers =new ArrayList<String>();
    List<String> backUppedContacts =new ArrayList<String>();
    List<String> backUppedNumbers =new ArrayList<String>();
    ListView mListView;
    MyAdapter mAdapter;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView =(ListView) findViewById(R.id.listView);
        mAdapter=new MyAdapter(this, contactsForAdapter, numbersForAdapter);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object obj = parent.getItemAtPosition(position);
                String phone = (String) obj;
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                startActivity(intent);
            }
        });
        showContacts();
    }
    private void updateContacts(String DisplayName, String MobileNumber,String HomeNumber)
    {
        ArrayList <ContentProviderOperation> cpo = new ArrayList <>();
        cpo.add(ContentProviderOperation.newInsert(
                ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());
        if (DisplayName != null) {
            cpo.add(ContentProviderOperation.newInsert(
                    ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,DisplayName).build());
        }
        if (MobileNumber != null) {
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, MobileNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());
        }
        if (HomeNumber != null) {
            cpo.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, HomeNumber)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                    .build());
        }
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, cpo);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void deleteAndUpdate() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
            contentResolver.delete(uri, null, null);
        }
        cursor.close();
        String dName= backUppedContacts.get(0);
        String mNumb= backUppedNumbers.get(0);
        String hNumb=null;
        int sentinel=0;
        for(int i=1;i< backUppedContacts.size();i++)
        {
            if(!dName.equals(backUppedContacts.get(i)))
            {
                updateContacts(dName,mNumb,hNumb);
                sentinel=0;
                hNumb=null;
                dName= backUppedContacts.get(i);
                mNumb= backUppedNumbers.get(i);
            }
            else
            {
                sentinel++;
                if(sentinel==1)
                {
                    hNumb= backUppedNumbers.get(i);
                }
            }
        }
        updateContacts(dName,mNumb,hNumb);
    }
    private void showContacts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            getContactNames();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void getContactNames() {
        internalContacts.clear();
        internalNumbers.clear();
        String[]s=new String[2];
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                s[0] = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        s[1] = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        s[1]=s[1].replace(" ","");
                        s[1]=s[1].replace("-","");
                        s[1]=s[1].replace("(","");
                        s[1]=s[1].replace(")","");
                        if(!internalNumbers.contains(s[1])) {
                            internalContacts.add(s[0]);
                            internalNumbers.add(s[1]);
                            if(!contactsForAdapter.contains(s[0]))
                            {
                                contactsForAdapter.add(s[0]);
                                numbersForAdapter.add(s[1]);
                            }
                        }

                    }
                    pCur.close();
                }
            }
            cur.close();
        }

        mAdapter.notifyDataSetChanged();
    }

    private void writeToFile()
    {
        try {
            File file=new File(getFilesDir().getAbsolutePath()) ;
            PrintWriter writer = new PrintWriter(new FileWriter(file+"ContactInfo.txt", false));
            for(int i=0;i< internalNumbers.size();i++)
            {
                writer.println(internalContacts.get(i)+"\t"+ internalNumbers.get(i));
                Log.v("write", internalContacts.get(i)+"\t"+internalNumbers.get(i));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void readFromFile()
    {
        try {
            backUppedContacts.clear();
            backUppedNumbers.clear();
            File file=new File(getFilesDir().getAbsolutePath()) ;
            FileReader fr = new FileReader(file+"ContactInfo.txt");
            BufferedReader br = new BufferedReader(fr);
            String str;
            while ((str=br.readLine())!=null)
            {
                Log.v("read",str);
                int index=str.indexOf('\t');
                String [] s=new String[2];
                s[0]=str.substring(0,index);
                s[1]=str.substring(index+1,str.length());
                backUppedContacts.add(s[0]);
                backUppedNumbers.add(s[1]);
            }
            br.close();
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void aveaClick(View view) {
        numbersForAdapter.clear();
        contactsForAdapter.clear();
        for(int i=0;i< internalNumbers.size();i++)
        {
            if(internalNumbers.get(i).startsWith("055")|| internalNumbers.get(i).startsWith("+9055")|| internalNumbers.get(i).startsWith("55"))
            {
                Log.v("avea", internalContacts.get(i)+"\t"+internalNumbers.get(i));
                contactsForAdapter.add(internalContacts.get(i));
                numbersForAdapter.add(internalNumbers.get(i));
            }
        }

        mAdapter.notifyDataSetChanged();
    }
    public void turkcellClick(View view) {
        numbersForAdapter.clear();
        contactsForAdapter.clear();
        for(int i=0;i< internalNumbers.size();i++)
        {
            if(internalNumbers.get(i).startsWith("053")|| internalNumbers.get(i).startsWith("+9053")|| internalNumbers.get(i).startsWith("53"))
            {
                Log.v("turkcell", internalContacts.get(i)+"\t"+internalNumbers.get(i));
                contactsForAdapter.add(internalContacts.get(i));
                numbersForAdapter.add(internalNumbers.get(i));
            }
        }
        mAdapter.notifyDataSetChanged();
    }
    public void vodafoneClick(View view) {
        numbersForAdapter.clear();
        contactsForAdapter.clear();
        for(int i=0;i< internalNumbers.size();i++)
        {
            if(internalNumbers.get(i).startsWith("054")|| internalNumbers.get(i).startsWith("+9054")|| internalNumbers.get(i).startsWith("54"))
            {
                Log.v("vodafone", internalContacts.get(i)+"\t"+internalNumbers.get(i));
                contactsForAdapter.add(internalContacts.get(i));
                numbersForAdapter.add(internalNumbers.get(i));
            }
        }
        mAdapter.notifyDataSetChanged();
    }
    public void allClick(View view) {
        numbersForAdapter.clear();
        contactsForAdapter.clear();
        for(int i=0;i< internalNumbers.size();i++)
        {
            if(!contactsForAdapter.contains(internalContacts.get(i)))
            {
                Log.v("all", internalContacts.get(i)+"\t"+internalNumbers.get(i));
                numbersForAdapter.add(internalNumbers.get(i));
                contactsForAdapter.add(internalContacts.get(i));
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void backClicked(View view) {
        writeToFile();
        Toast.makeText(this, "Back-upped",
                Toast.LENGTH_SHORT).show();
    }

    public void recoverClicked(View view) {
        File file=new File(getFilesDir().getAbsolutePath()) ;
        try {
            FileReader fr = new FileReader(file+"ContactInfo.txt");
            readFromFile();
            deleteAndUpdate();
            showContacts();
            RadioButton r=(RadioButton)findViewById(R.id.radioButtonAll);
            r.setChecked(true);
            Toast.makeText(this, "Recovered",
                    Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "You should take a back-up!",
                    Toast.LENGTH_SHORT).show();
        }

    }
    public class MyAdapter extends BaseAdapter {
        private LayoutInflater myInflater;
        private List<String> myContacts;
        private List<String> myNumbers;

        public MyAdapter(MainActivity activity, List<String> contacts, List<String> numbers){
            myInflater=(LayoutInflater) activity.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            myContacts=contacts;
            myNumbers=numbers;

        }

        @Override
        public int getCount() {
            return myContacts.size();
        }

        @Override
        public Object getItem(int position) {
            return myNumbers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView=myInflater.inflate(R.layout.list_layout,null);
            TextView textView =
                    (TextView) rowView.findViewById(R.id.textView);
            TextView textView2 =
                    (TextView) rowView.findViewById(R.id.textView2);
            ImageView imageView =
                    (ImageView) rowView.findViewById(R.id.imageViewId);
            textView.setText(myContacts.get(position));
            textView2.setText(myNumbers.get(position));
            imageView.setImageResource(R.drawable.photo);

            return rowView;

        }
    }
}
