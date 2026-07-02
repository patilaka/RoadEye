package com.carassistant.managers;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SharedPreferencesManager_Factory implements Factory<SharedPreferencesManager> {
  private final Provider<Context> contextProvider;

  public SharedPreferencesManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SharedPreferencesManager get() {
    return newInstance(contextProvider.get());
  }

  public static SharedPreferencesManager_Factory create(Provider<Context> contextProvider) {
    return new SharedPreferencesManager_Factory(contextProvider);
  }

  public static SharedPreferencesManager newInstance(Context context) {
    return new SharedPreferencesManager(context);
  }
}
