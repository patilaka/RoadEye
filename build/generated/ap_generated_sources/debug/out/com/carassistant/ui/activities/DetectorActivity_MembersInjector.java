package com.carassistant.ui.activities;

import com.carassistant.managers.SharedPreferencesManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DetectorActivity_MembersInjector implements MembersInjector<DetectorActivity> {
  private final Provider<SharedPreferencesManager> sharedPreferencesManagerProvider;

  public DetectorActivity_MembersInjector(
      Provider<SharedPreferencesManager> sharedPreferencesManagerProvider) {
    this.sharedPreferencesManagerProvider = sharedPreferencesManagerProvider;
  }

  public static MembersInjector<DetectorActivity> create(
      Provider<SharedPreferencesManager> sharedPreferencesManagerProvider) {
    return new DetectorActivity_MembersInjector(sharedPreferencesManagerProvider);
  }

  @Override
  public void injectMembers(DetectorActivity instance) {
    injectSharedPreferencesManager(instance, sharedPreferencesManagerProvider.get());
  }

  @InjectedFieldSignature("com.carassistant.ui.activities.DetectorActivity.sharedPreferencesManager")
  public static void injectSharedPreferencesManager(DetectorActivity instance,
      SharedPreferencesManager sharedPreferencesManager) {
    instance.sharedPreferencesManager = sharedPreferencesManager;
  }
}
