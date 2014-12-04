/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.sitata.googleplus;

import java.util.HashMap;
import java.util.logging.Logger;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupport;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;



@Kroll.module(name="TitaniumGooglePlus", id="com.sitata.googleplus")
public class TitaniumGooglePlusModule extends KrollModule implements
		TiActivityResultHandler,
		com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks,
		com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
{

	// User did not pick account properly / hit cancel on dialog
	@Kroll.property
	public static final String SIGN_IN_CANCELLED = "tigp:signInCancelled";

	// There was an error during the sign in process or the user did not grant
	// permissions
	@Kroll.property
	public static final String SIGN_IN_ERROR = "tigp:signInError";

	// could be network is down
	@Kroll.constant
	public static final String IO_EXCEPTION = "tigp:ioException";

	// Some other type of unrecoverable exception has occurred.
	// Report and log the error as appropriate for your app.
	@Kroll.constant
	public static final String FATAL_EXCEPTION = "tigp:fatalException";
	
	// exposing Scope constants
	@Kroll.constant public static final String SCOPE_LOGIN = "login";
	@Kroll.constant public static final String SCOPE_PROFILE = "profile";

	// Standard Debugging variables
	private static final String TAG = "TitaniumGooglePlusModule";

	protected int recoveryRequestCode;
	private static final int RC_SIGN_IN = 99;
	private String mClientId;
	private String[] mScopes;
	private String mEmail;
	private KrollFunction successCallback;
	private KrollFunction errorCallback;

	/* Client used to interact with Google APIs. */
	private static GoogleApiClient mGoogleApiClient;

	private Boolean mIntentInProgress = false;
	private Boolean mClearingAccount = false;

	public TitaniumGooglePlusModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(TAG, "inside onAppCreate");
		// put module init code that needs to run when the application is created
	}

	@Override
	public void onStart(Activity activity) {
		super.onStart(activity);
		// We don't want to put the mGoogleApiClient.connect() here as it might
		// trigger an account chooser dialog before we actually want it.
	}

	@Override
	public void onStop(Activity activity) {
		super.onStop(activity);
		if (mGoogleApiClient.isConnected()) {
			Log.d(TAG, "On stop and disconnecting");
			mGoogleApiClient.disconnect();
		}
	}


	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG, "OnConnectionFailed");

		// Called when there was an error connecting the client to the service.

		if (!mIntentInProgress && result.hasResolution()) {

			mIntentInProgress = true;

			// allow Google Play services to solicit any user interaction needed
			// to resolve sign in errors (for example by asking the user to
			// select an account, consent to permissions, enable networking,
			// etc).
			Activity activity = TiApplication.getAppCurrentActivity();
			TiActivitySupport support = (TiActivitySupport) activity;
			support.launchIntentSenderForResult(result.getResolution()
					.getIntentSender(), RC_SIGN_IN, null, 0, 0, 0, null, this);
		}
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(TAG, "Connected!");
		// We've resolved any connection errors. mGoogleApiClient can be used
		// to access Google APIs on behalf of the user.
		
		// After calling connect(), this method will be invoked asynchronously
		// when the connect request has successfully completed.
		if (mClearingAccount) {
			// if we intended to clear the account, mClearingAccount will have
			// been set to true prior to connecting
			clearAccount();
		} else {
			
			mEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
			
			if (hasListeners("login")) {
				Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
				
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("id", person.getId());
				data.put("email", mEmail);
				data.put("image", person.getImage().getUrl());
				data.put("name", person.getName().getFormatted());
				data.put("givenName", person.getName().getGivenName());
				data.put("familyName", person.getName().getFamilyName());
				data.put("nickname", person.getNickname());
				data.put("gender", person.getGender());
				data.put("about", person.getAboutMe());
				data.put("birthday", person.getBirthday());
				data.put("url", person.getUrl());
				data.put("currentLocation", person.getCurrentLocation());
				data.put("isPlusUser", person.isPlusUser());
				
				HashMap<String, Object> event = new HashMap<String, Object>();
				event.put("success", true);
				event.put("data", data);
				fireEvent("login", event);
			}
			
		}
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.d(TAG, "On Suspended!");
		// Google Play services will trigger the onConnectionSuspended callback
		// if our Activity loses its service connection. Typically you will want
		// to attempt to reconnect when this happens in order to retrieve a new
		// ConnectionResult that can be resolved by the user.
		mGoogleApiClient.connect();
	}

	@Override
	public void onError(Activity activity, int requestCode, Exception e) {
		Logger.getLogger(TAG).info("ON ERROR called. " + requestCode);
		Log.d(TAG, "EXCEPTION: " + e.getMessage());

		if (requestCode == RC_SIGN_IN) {
			handleError(SIGN_IN_ERROR);
		}

	}

	@Override
	public void onResult(Activity activity, int thisRequestCode,
			int resultCode, Intent data) {
		Logger.getLogger(TAG).info(
				"On Result - Request Code is: " + thisRequestCode
						+ " - Result Code is: " + resultCode);


		if (thisRequestCode == RC_SIGN_IN) {
			mIntentInProgress = false;
			// At this point, the user has chosen an account and we need to try
			// to connect again

			// 0 when cancelled picking an account or when refusing permissions
			// -1 when picked account and already gave permissions

			if (resultCode == Activity.RESULT_OK) {
				// Because the resolution for the connection failure was started
				// with startIntentSenderForResult (launchIntentSenderForResult in
				// Titanium) and the code RC_SIGN_IN, we can
				// capture the result inside Activity.onActivityResult.
				if (!mGoogleApiClient.isConnecting()) {
					Log.d(TAG, "Connecting again");
					mGoogleApiClient.connect();
				}
			} else {
				handleError(SIGN_IN_CANCELLED);
			}

		} else if (thisRequestCode == recoveryRequestCode) {
			// At this point, the user has chosen an account and we have
			// attempted to fetch information
			// but the user might have needed to grant permissions still and
			// we're coming back from that process.
			Logger.getLogger(TAG).info("Handling Recovery Request Result.");
			if (resultCode == Activity.RESULT_OK) {
				Bundle extra = data.getExtras();
				String oneTimeToken = extra.getString("authtoken");
				handleSignInSuccess(mEmail, oneTimeToken);
			} else {
				// if we made it to the permissions screen, the user might have
				// made a mistake on picking
				// the correct user account, so let's clear it first
				handleClearAccount();
				handleError(SIGN_IN_CANCELLED);
			}
		}
	}

	private void handleSignInSuccess(String email, String token) {
		HashMap<String, String> event = new HashMap<String, String>();
		event.put("accountId", email);
		event.put("accessToken", token);
		successCallback.call(getKrollObject(), event);

	}

	// Clear out the account selection
	private void clearAccount() {
		if (mGoogleApiClient.isConnected()) {
			Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
			mGoogleApiClient.disconnect();
		}
		mClearingAccount = false;
	}

	private void handleClearAccount() {
		// Reset the google api client and clear out any account selections
		// in case the user wants to pick a different one.
		mClearingAccount = true;
		if (!mGoogleApiClient.isConnected()) {
			mGoogleApiClient.connect();
		} else {
			clearAccount();
		}
	}


	private void handleError(String code) {
		mIntentInProgress = false;

		HashMap<String, Object> event = new HashMap<String, Object>();
		event.put("error", code);
		errorCallback.call(getKrollObject(), event);
		
		if (hasListeners("login")) {
			event.put("success", false);
			fireEvent("login", event);
		}
		
	}

	// Build the google api client
	private void buildClient() {
		Activity activity = TiApplication.getAppCurrentActivity();
		mGoogleApiClient = new GoogleApiClient.Builder(activity)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_LOGIN)
				.build();
	}

	// Methods
	@Kroll.method
	public void signin()
	{
		if (mGoogleApiClient == null) {
			buildClient();
		}

		if (!mGoogleApiClient.isConnected()) {
			mClearingAccount = false;
			mGoogleApiClient.connect();
		}
	}

	@Kroll.method
	public void signout() {
		
		if(mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
                mGoogleApiClient.connect();
            }
        }
		
		successCallback = null;
		errorCallback = null;
	}

	@Kroll.method
	public void disconnect() {
		mGoogleApiClient.disconnect();
	}

	@Kroll.method
	public Boolean isLoggedIn() {
		return mGoogleApiClient != null && mGoogleApiClient.isConnected();
	}

	// Properties
	@Kroll.getProperty @Kroll.method
	public void setClientId(String value)
	{
		// This method is simply a stub for iOS since we don't
		// need a clientId;
		mClientId = value;
	}

	@Kroll.getProperty @Kroll.method
	public String getClientId() {
		// This method is simply a stub for iOS since we don't
		// need a clientId;
		return mClientId;
	}

	@Kroll.setProperty @Kroll.method
	public void setScopes(String[] value)
	{
		mScopes = value;
	}

	@Kroll.getProperty @Kroll.method
	public Object[] getScopes() {
		return mScopes;
	}

}

