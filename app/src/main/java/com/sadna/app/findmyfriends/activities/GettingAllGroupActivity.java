package com.sadna.app.findmyfriends.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sadna.app.findmyfriends.MyApplication;
import com.sadna.app.findmyfriends.R;
import com.sadna.app.findmyfriends.entities.UserPhone;
import com.sadna.app.webservice.WebService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class GettingAllGroupActivity extends Activity {

    private ArrayList<String> Phones = new ArrayList<String>();
    private Map <String,String> map =  new HashMap<String,String>();
    private Map <String,String> mapAfterFindingMatches =  new HashMap<String,String>();
    private Vector<String> namesOfContacts = new Vector<String>();
    private ArrayList<UserPhone> PhonesThatAreConnectedWithApp = new ArrayList<>();
    private Gson gson = new Gson();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_all_group);
        getAllContactsFromUser();
        GetPhonesFromDataBase();
        final ListView resultListView = (ListView) findViewById(R.id.ListOfContacts);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, namesOfContacts);
        resultListView.setAdapter(adapter);
        resultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // ListView Clicked item index
                int itemPosition = position;
                // ListView Clicked item value
                final String itemValue = (String) resultListView.getItemAtPosition(position);
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(GettingAllGroupActivity.this);
                mBuilder.setMessage("You sure that you want to add this person to the group")
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // if this button is clicked, just close
                                // the dialog box and do nothing
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        adapter.remove(itemValue);
                                        String userName = getUsernameFromUserContacts(itemValue);
                                        setUserOnGroupDataBase(userName);
                                    }
                                }
            );
            AlertDialog alert = mBuilder.create();
            alert.show();

        }
    });
        final Button finishButton = (Button)findViewById(R.id.finisnbutton);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), GroupsMainActivity.class));
            }
        });
    }


    private String getUsernameFromUserContacts(String userContact) {
        String phone = null;
        String userName = null;
        for (Map.Entry<String, String> entry : mapAfterFindingMatches.entrySet()) {
            if (userContact.equals(entry.getValue())) {
                phone = entry.getKey();
                break;
            }
        }
        for( UserPhone user :PhonesThatAreConnectedWithApp)
        {
            if(phone.equals(user.getPhone()))
            {
                userName = user.getUsername();
                break;
            }
        }
        return userName;
    }
    private void getAllContactsFromUser()
    {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {

                if ((Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))))==1) {
                    String id = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(
                            ContactsContract.Contacts.DISPLAY_NAME));
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                                    + " = ?", new String[]{id}, null);
                    int i = 0;
                    int pCount = pCur.getCount();
                    String[] phoneNum = new String[pCount];
                    String[] phoneType = new String[pCount];

                    while (pCur != null && pCur.moveToNext()) {
                        phoneNum[i] = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneType[i] = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.TYPE));
                        String newStringOrder = orderPhoneNumbers(phoneNum[i]);
                        if(newStringOrder != null) {
                            Phones.add(newStringOrder);
                            map.put(newStringOrder, name);
                        }
                        i++;

                    }
                }

            }
        }
    }

    private String orderPhoneNumbers(String unorderPhoneNumber) {
        StringBuilder newOrderString = new StringBuilder(unorderPhoneNumber);
        String stringToReturn;

        if (newOrderString.length() <= 10 || newOrderString.substring(0,3).compareTo("077") == 0 || newOrderString.substring(0,3).compareTo("072") == 0) {
            stringToReturn = null;
        } else {
            newOrderString = new StringBuilder(newOrderString.toString().replaceAll("\\s", ""));
            newOrderString = new StringBuilder (newOrderString.toString().replaceAll("-", ""));
            String result = newOrderString.substring(0,4);
            if (result.compareTo("+972") == 0) {
                newOrderString = new StringBuilder(newOrderString.replace(0, 4, "0"));
            }
            stringToReturn = newOrderString.toString();
        }
        return stringToReturn;
    }

    private void setUserOnGroupDataBase(final String userName) {
        Thread addUserToGroupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("addUserToGroup");
                    String result = null;

                    try {
                        result = wsHttpRequest.execute(((MyApplication)getApplication()).getSelectedGroupId(),userName);
                    } catch (Throwable exception) {
                        Log.e("GettingAllGroupActivity" ,exception.getMessage());
                    }
                } catch (Exception e) {
                    Log.e("GroupsMainActivity", e.getMessage());
                }
            }
        });

        addUserToGroupThread.start();
        try {
            addUserToGroupThread.join();
        } catch (InterruptedException exception) {
            Log.e("GettingAllGroupActivity", exception.getMessage());
        }
    }

    private void GetPhonesFromDataBase() {
        Thread getUserPhonesThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebService wsHttpRequest = new WebService("getPhonesFromContacts");
                    String result = null;

                    try {
                        result = wsHttpRequest.executeToArray(Phones);
                    } catch (Throwable exception) {
                        Log.e("GettingAllGroupActivity" ,exception.getMessage());
                    }

                    PhonesThatAreConnectedWithApp = gson.fromJson(result, new TypeToken<ArrayList<UserPhone>>() {
                    }.getType());
                    compareMapWithPhoneContacts();
                } catch (Exception e) {
                    Log.e("GroupsMainActivity", e.getMessage());
                }
            }
        });

        getUserPhonesThread.start();
        try {
            getUserPhonesThread.join();
        } catch (InterruptedException exception) {
            Log.e("GroupsMainActivity", exception.getMessage());
        }
    }


    private void compareMapWithPhoneContacts()
    {
        for(UserPhone phone : PhonesThatAreConnectedWithApp)
        {
            for(Map.Entry<String, String> contact : map.entrySet())
            {
                String phoneToCompare = contact.getKey();
                String PhoneOfUser = phone.getPhone();
                if(PhoneOfUser.equals(phoneToCompare))
                {
                    String name = contact.getValue();
                    mapAfterFindingMatches.put(phoneToCompare, name);
                    namesOfContacts.add(name);
                    break;
                }
            }
        }

    }

}