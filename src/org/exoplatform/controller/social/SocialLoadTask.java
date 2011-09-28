package org.exoplatform.controller.social;

import java.util.ArrayList;

import org.exoplatform.model.SocialActivityInfo;
import org.exoplatform.singleton.LocalizationHelper;
import org.exoplatform.singleton.SocialServiceHelper;
import org.exoplatform.social.client.api.common.RealtimeListAccess;
import org.exoplatform.social.client.api.model.RestActivity;
import org.exoplatform.social.client.api.model.RestIdentity;
import org.exoplatform.social.client.api.model.RestProfile;
import org.exoplatform.social.client.api.service.ActivityService;
import org.exoplatform.social.client.api.service.IdentityService;
import org.exoplatform.utils.UserTask;
import org.exoplatform.widget.WaitingDialog;
import org.exoplatform.widget.WarningDialog;

import android.content.Context;

public class SocialLoadTask extends UserTask<Integer, Void, ArrayList<SocialActivityInfo>> {
  private SocialWaitingDialog _progressDialog;

  private Context             mContext;

  private String              loadingData;

  private String              okString;

  private String              titleString;

  private String              contentString;

  private SocialController    socialController;

  public SocialLoadTask(Context context, SocialController controller) {
    mContext = context;
    socialController = controller;
    changeLanguage();
  }

  private void changeLanguage() {
    LocalizationHelper location = LocalizationHelper.getInstance();
    loadingData = location.getString("LoadingData");
    okString = location.getString("OK");
    titleString = location.getString("Warning");
    contentString = location.getString("ConnectionError");
  }

  @Override
  public void onPreExecute() {
    _progressDialog = new SocialWaitingDialog(mContext, null, loadingData);
    _progressDialog.show();

  }

  @Override
  public ArrayList<SocialActivityInfo> doInBackground(Integer... params) {

    try {
      ArrayList<SocialActivityInfo> streamInfoList = new ArrayList<SocialActivityInfo>();

      int loadSize = params[0];
      ActivityService<RestActivity> activityService = SocialServiceHelper.getInstance()
                                                                         .getActivityService();
      IdentityService<?> identityService = SocialServiceHelper.getInstance().getIdentityService();
      RestIdentity identity = (RestIdentity) identityService.get(SocialServiceHelper.getInstance()
                                                                                    .getUserId());
      RealtimeListAccess<RestActivity> list = activityService.getActivityStream(identity);
      ArrayList<RestActivity> activityList = (ArrayList<RestActivity>) list.loadAsList(0, loadSize);
      SocialActivityInfo streamInfo = null;
      RestProfile profile = null;
      for (RestActivity act : activityList) {
        streamInfo = new SocialActivityInfo();
        profile = act.getPosterIdentity().getProfile();
        streamInfo.setActivityId(act.getId());
        streamInfo.setImageUrl(profile.getAvatarUrl());
        streamInfo.setUserName(profile.getFullName());
        streamInfo.setTitle(act.getTitle());
        streamInfo.setPostedTime(act.getPostedTime());
        streamInfo.setLikeNumber(act.getLikes().size());
        streamInfo.setCommentNumber(act.getAvailableComments().size());
        streamInfoList.add(streamInfo);
      }
      return streamInfoList;
    } catch (RuntimeException e) {
      return null;
    }
  }

  @Override
  public void onCancelled() {
    super.onCancelled();
    _progressDialog.dismiss();
  }

  @Override
  public void onPostExecute(ArrayList<SocialActivityInfo> result) {

    if (result != null) {
      socialController.setActivityList(result);
    } else {
      WarningDialog dialog = new WarningDialog(mContext, titleString, contentString, okString);
      dialog.show();
    }
    _progressDialog.dismiss();

  }

  private class SocialWaitingDialog extends WaitingDialog {

    public SocialWaitingDialog(Context context, String titleString, String contentString) {
      super(context, titleString, contentString);
    }

    @Override
    public void onBackPressed() {
      super.onBackPressed();
      socialController.onCancelLoad();
    }

  }

}
