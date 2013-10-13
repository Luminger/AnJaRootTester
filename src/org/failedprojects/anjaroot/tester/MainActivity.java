/*
 * Copyright 2013 Simon Brakhane
 *
 * This file is part of AnJaRoot.
 *
 * AnJaRoot is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * AnJaRoot is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * AnJaRoot. If not, see http://www.gnu.org/licenses/.
 */
package org.failedprojects.anjaroot.tester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.failedprojects.anjaroot.library.AnJaRoot;
import org.failedprojects.anjaroot.library.AnJaRootRequester;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements
		AnJaRootRequester.ConnectionStatusListener,
		AnJaRootRequester.AsyncRequestHandler {
	private boolean connected = false;
	private AnJaRootRequester requester;
	private ProgressDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// normal onCreate business
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// instantiate a new requester, needed to request access if it's not
		// already granted.
		requester = new AnJaRootRequester(MainActivity.this, this);

		// prepare dialog we want to show wile the request is in progress
		dialog = new ProgressDialog(this);
		dialog.setTitle("Working");
		dialog.setMessage("Requesting access...");
		dialog.setIndeterminate(true);

		// prepare the suicide button
		Button suicide = (Button) findViewById(R.id.restart_btn);
		suicide.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,
						MainActivity.class);
				PendingIntent pending = PendingIntent.getActivity(
						MainActivity.this, 0, intent,
						Intent.FLAG_ACTIVITY_NEW_TASK);

				AnJaRoot.commitSuicideAndRestart(MainActivity.this, pending);
			}
		});

		// prepare the request button
		Button request = (Button) findViewById(R.id.issue_request);
		request.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.show();

				if (connected) {
					requester.requestAccessAsync(MainActivity.this);
				}
			}
		});

		// TODO rename this library functions, they are not named properly
		if (AnJaRoot.isAccessPossible() && AnJaRoot.isAccessGranted()) {
			// Display UI which lets the user issue commands
			findViewById(R.id.functional).setVisibility(View.VISIBLE);
			findViewById(R.id.execute_id_command).setVisibility(View.VISIBLE);
			findViewById(R.id.get_packages_list).setVisibility(View.VISIBLE);

			// Hide the requesting UI
			findViewById(R.id.access_not_granted).setVisibility(View.GONE);
			findViewById(R.id.issue_request).setVisibility(View.GONE);
		}

		// prepare the "get id" button
		Button id = (Button) findViewById(R.id.execute_id_command);
		id.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean success = AnJaRoot.gainAccess();
				if (!success) {
					Toast.makeText(MainActivity.this, "Gaining Access failed!",
							Toast.LENGTH_LONG).show();
				}

				try {
					Process p = Runtime.getRuntime().exec("id");
					InputStream in = p.getInputStream();
					InputStreamReader inreader = new InputStreamReader(in);
					BufferedReader reader = new BufferedReader(inreader);

					Toast.makeText(MainActivity.this, reader.readLine(),
							Toast.LENGTH_LONG).show();
					p.waitFor();

				} catch (Exception e) {
					Toast.makeText(MainActivity.this,
							String.format("Error: %s", e.toString()),
							Toast.LENGTH_LONG).show();
				}

				AnJaRoot.dropAccess();
			}
		});

		// prepare the "get uids" Button Button uids = (Button)
		Button packages = (Button) findViewById(R.id.get_packages_list);
		packages.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean success = AnJaRoot.gainAccess();
				if (!success) {
					Toast.makeText(MainActivity.this, "Gaining Access failed!",
							Toast.LENGTH_LONG).show();
				}

				try {
					BufferedReader reader = new BufferedReader(new FileReader(
							"/data/system/packages.list"));

					Toast.makeText(MainActivity.this, reader.readLine(),
							Toast.LENGTH_LONG).show();

					reader.close();

				} catch (IOException e) {
					Toast.makeText(MainActivity.this,
							String.format("Error: %s", e.toString()),
							Toast.LENGTH_LONG).show();
				}

				AnJaRoot.dropAccess();
			}
		});
	}

	@Override
	public void onConnectionStatusChange(boolean connected) {
		this.connected = connected;
	}

	@Override
	public void onReturn(boolean granted) {
		dialog.dismiss();

		if (granted) {
			findViewById(R.id.access_not_granted).setVisibility(View.GONE);
			findViewById(R.id.issue_request).setVisibility(View.GONE);

			findViewById(R.id.restart_text).setVisibility(View.VISIBLE);
			findViewById(R.id.restart_btn).setVisibility(View.VISIBLE);
		}

		Toast.makeText(this, String.format("Request answered: %b", granted),
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onServiceUnrechable() {
		dialog.dismiss();

		Toast.makeText(this,
				"Failed to connect to service, you should retry it now.",
				Toast.LENGTH_LONG).show();
	}

}
