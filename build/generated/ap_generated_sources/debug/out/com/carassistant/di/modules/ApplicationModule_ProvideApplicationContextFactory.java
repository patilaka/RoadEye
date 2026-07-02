package com.carassistant.di.modules;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ApplicationModule_ProvideApplicationContextFactory implements Factory<Context> {
  private final ApplicationModule module;

  public ApplicationModule_ProvideApplicationContextFactory(ApplicationModule module) {
    this.module = module;
  }

  @Override
  public Context get() {
    return provideApplicationContext(module);
  }

  public static ApplicationModule_ProvideApplicationContextFactory create(
      ApplicationModule module) {
    return new ApplicationModule_ProvideApplicationContextFactory(module);
  }

  public static Context provideApplicationContext(ApplicationModule instance) {
    return Preconditions.checkNotNullFromProvides(instance.provideApplicationContext());
  }
}
