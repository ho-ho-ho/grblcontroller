/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller.listeners;


import android.net.Uri;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import in.co.gorest.grblcontroller.BR;
import in.co.gorest.grblcontroller.model.Bounds;

public class FileSenderListener extends BaseObservable {

    private String gcodeFileName;
    private Uri gcodeUri;
    private Integer rowsInFile;
    private Integer rowsSent;

    private String status;
    private Bounds bounds;

    public static final String STATUS_IDLE = "Idle";
    public static final String STATUS_READING = "Reading";
    public static final String STATUS_STREAMING = "Streaming";

    private long jobStartTime = 0L;
    private long jobEndTime = 0L;
    private String elapsedTime = "00:00:00";

    private static FileSenderListener fileSenderListener = null;

    public static FileSenderListener getInstance() {
        if (fileSenderListener == null) {
            fileSenderListener = new FileSenderListener();
        }
        return fileSenderListener;
    }

    public static void resetClass() {
        fileSenderListener = new FileSenderListener();
    }

    private FileSenderListener() {
        this.setStatus(STATUS_IDLE);
        this.bounds = null;
        this.gcodeFileName = "";
        //this.gcodeFile = null;
        this.gcodeUri = null;
        this.rowsInFile = 0;
        this.rowsSent = 0;
    }

    @Bindable
    public Bounds getBounds() {
        return this.bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
        notifyPropertyChanged(BR.bounds);
    }

    @Bindable
    public String getGcodeFileName() {
        return this.gcodeFileName;
    }

    public void setGcodeFileName(String gcodeFileName) {
        this.gcodeFileName = gcodeFileName;
        notifyPropertyChanged(BR.gcodeFileName);
    }

    @Bindable
    public Uri getGcodeUri() {
        return this.gcodeUri;
    }

    public void setGcodeUri(Uri uri) {
        this.gcodeUri = uri;
        notifyPropertyChanged(BR.gcodeUri);
    }

    @Bindable
    public Integer getRowsInFile() {
        return this.rowsInFile;
    }

    public void setRowsInFile(Integer rowsInFile) {
        this.rowsInFile = rowsInFile;
        notifyPropertyChanged(BR.rowsInFile);
    }

    @Bindable
    public Integer getRowsSent() {
        return this.rowsSent;
    }

    public void setRowsSent(Integer rowsSent) {
        this.rowsSent = rowsSent;
        notifyPropertyChanged(BR.rowsSent);
    }

    @Bindable
    public long getJobStartTime() {
        return this.jobStartTime;
    }

    public void setJobStartTime(long startTime) {
        this.jobStartTime = startTime;
        notifyPropertyChanged(BR.jobStartTime);
    }

    @Bindable
    public long getJobEndTime() {
        return this.jobEndTime;
    }

    public void setJobEndTime(long endTime) {
        this.jobEndTime = endTime;
        notifyPropertyChanged(BR.jobEndTime);
    }

    @Bindable
    public String getElapsedTime() {
        return this.elapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
        notifyPropertyChanged(BR.elapsedTime);
    }

    @Bindable
    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }

}
