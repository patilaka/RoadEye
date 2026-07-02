package com.carassistant.di.components;

import com.carassistant.ui.activities.DetectorActivity;
import com.carassistant.ui.activities.DetectorActivity_MembersInjector;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dagger.internal.DaggerGenerated;
import dagger.internal.Preconditions;
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
public final class DaggerScreenComponent {
  private DaggerScreenComponent() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationComponent applicationComponent;

    private Builder() {
    }

    public Builder applicationComponent(ApplicationComponent applicationComponent) {
      this.applicationComponent = Preconditions.checkNotNull(applicationComponent);
      return this;
    }

    public ScreenComponent build() {
      Preconditions.checkBuilderRequirement(applicationComponent, ApplicationComponent.class);
      return new ScreenComponentImpl(applicationComponent);
    }
  }

  private static final class ScreenComponentImpl implements ScreenComponent {
    private final ApplicationComponent applicationComponent;

    private final ScreenComponentImpl screenComponentImpl = this;

    private ScreenComponentImpl(ApplicationComponent applicationComponentParam) {
      this.applicationComponent = applicationComponentParam;

    }

    @Override
    public void inject(DetectorActivity detectorActivity) {
      injectDetectorActivity(detectorActivity);
    }

    @CanIgnoreReturnValue
    private DetectorActivity injectDetectorActivity(DetectorActivity instance) {
      DetectorActivity_MembersInjector.injectSharedPreferencesManager(instance, Preconditions.checkNotNullFromComponent(applicationComponent.sharedPreferences()));
      return instance;
    }
  }
}
