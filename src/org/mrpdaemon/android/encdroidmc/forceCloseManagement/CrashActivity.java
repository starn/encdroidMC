/*
 * Copyright (C) 2007-2011 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.mrpdaemon.android.encdroidmc.forceCloseManagement;


import org.mrpdaemon.android.encdroidmc.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;


public class CrashActivity extends Activity {
	static final String STACKTRACE = "stacktrace";

	public void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		
		setContentView(R.layout.error_layout);
		
		final String stackTrace = getIntent().getStringExtra(STACKTRACE);
		final TextView reportTextView = (TextView)findViewById(R.id.test);
		reportTextView.setMovementMethod(new ScrollingMovementMethod());

		reportTextView.setText(stackTrace);
	}
}
