package com.habitrpg.android.habitica.interactors;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.events.ShareEvent;
import com.habitrpg.android.habitica.executors.PostExecutionThread;
import com.habitrpg.android.habitica.executors.ThreadExecutor;
import com.habitrpg.android.habitica.helpers.SoundManager;
import com.habitrpg.android.habitica.ui.AvatarView;
import com.magicmicky.habitrpgwrapper.lib.models.HabitRPGUser;
import com.magicmicky.habitrpgwrapper.lib.models.SuppressedModals;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

import rx.Observable;

public class LevelUpUseCase extends UseCase<LevelUpUseCase.RequestValues, Void> {

    private SoundManager soundManager;
    private CheckClassSelectionUseCase checkClassSelectionUseCase;

    @Inject
    public LevelUpUseCase(SoundManager soundManager, ThreadExecutor threadExecutor, PostExecutionThread postExecutionThread,
                          CheckClassSelectionUseCase checkClassSelectionUseCase) {
        super(threadExecutor, postExecutionThread);
        this.soundManager = soundManager;
        this.checkClassSelectionUseCase = checkClassSelectionUseCase;
    }

    @Override
    protected Observable buildUseCaseObservable(RequestValues requestValues) {
        return Observable.from(() -> {
            soundManager.loadAndPlayAudio(SoundManager.SoundLevelUp);


            SuppressedModals suppressedModals = requestValues.user.getPreferences().getSuppressModals();
            if (suppressedModals != null) {
                if (suppressedModals.getLevelUp()) {
                    checkClassSelectionUseCase.observable(new CheckClassSelectionUseCase.RequestValues(requestValues.user, null))
                            .subscribe(aVoid -> {
                            }, throwable -> {
                            });

                    return null;
                }
            }

            View customView = requestValues.compatActivity.getLayoutInflater().inflate(R.layout.dialog_levelup, null);
            if (customView != null) {
                TextView detailView = (TextView) customView.findViewById(R.id.levelupDetail);
                detailView.setText(requestValues.compatActivity.getString(R.string.levelup_detail, requestValues.newLevel));
                AvatarView dialogAvatarView = (AvatarView) customView.findViewById(R.id.avatarView);
                dialogAvatarView.setUser(requestValues.user);
            }

            final ShareEvent event = new ShareEvent();
            event.sharedMessage = requestValues.compatActivity.getString(R.string.share_levelup, requestValues.newLevel + "") + " https://habitica.com/social/level-up";
            AvatarView avatarView = new AvatarView(requestValues.compatActivity, true, true, true);
            avatarView.setUser(requestValues.user);
            avatarView.onAvatarImageReady(avatarImage -> event.shareImage = avatarImage);

            AlertDialog alert = new AlertDialog.Builder(requestValues.compatActivity)
                    .setTitle(R.string.levelup_header)
                    .setView(customView)
                    .setPositiveButton(R.string.levelup_button, (dialog, which) -> {
                        checkClassSelectionUseCase.observable(new CheckClassSelectionUseCase.RequestValues(requestValues.user, null))
                                .subscribe(aVoid -> {
                                }, throwable -> {
                                });
                    })
                    .setNeutralButton(R.string.share, (dialog, which) -> {
                        EventBus.getDefault().post(event);
                        dialog.dismiss();
                    })
                    .create();

            if (!requestValues.compatActivity.isFinishing()) {
                alert.show();
            }

            return null;

        });
    }

    public static final class RequestValues implements UseCase.RequestValues {
        private HabitRPGUser user;
        private int newLevel;
        private AppCompatActivity compatActivity;

        public RequestValues(HabitRPGUser user, int newLevel, AppCompatActivity compatActivity) {

            this.user = user;
            this.newLevel = newLevel;
            this.compatActivity = compatActivity;
        }
    }
}
