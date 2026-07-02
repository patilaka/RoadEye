package com.carassistant.di.components;

import android.content.Context;
import com.carassistant.di.modules.ApplicationModule;
import com.carassistant.di.modules.ApplicationModule_ProvideApplicationContextFactory;
import com.carassistant.managers.SharedPreferencesManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import javax.annotation.processing.Generated;

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
public final class DaggerApplicationComponent {
  private DaggerApplicationComponent() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationModule applicationModule;

    private Builder() {
    }

    public Builder applicationModule(ApplicationModule applicationModule) {
      this.applicationModule = Preconditions.checkNotNull(applicationModule);
      return this;
    }

    public ApplicationComponent build() {
      Preconditions.checkBuilderRequirement(applicationModule, ApplicationModule.class);
      return new ApplicationComponentImpl(applicationModule);
    }
  }

  private static final class ApplicationComponentImpl implements ApplicationComponent {
    private final ApplicationComponentImpl applicationComponentImpl = this;

    private Provider<Context> provideApplicationContextProvider;

    private ApplicationComponentImpl(ApplicationModule applicationModuleParam) {

      initialize(applicationModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationModule applicationModuleParam) {
      this.provideApplicationContextProvider = DoubleCheck.provider(ApplicationModule_ProvideApplicationContextFactory.create(applicationModuleParam));
    }

    @Override
    public Context context() {
      return provideApplicationContextProvider.get();
    }

    @Override
    public SharedPreferencesManager sharedPreferences() {
      return new SharedPreferencesManager(provideApplicationContextProvider.get());
    }
  }
}
