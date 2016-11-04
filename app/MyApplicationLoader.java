import controllers.HomeController;
import dagger.Component;
import dagger.Module;
import dagger.Provides;
import play.Application;
import play.ApplicationLoader;
import play.DefaultApplication;
import play.Environment;
import play.api.LoggerConfigurator$;
import play.api.OptionalSourceMapper;
import play.api.http.DefaultHttpFilters;
import play.api.http.HttpRequestHandler;
import play.api.http.JavaCompatibleHttpRequestHandler;
import play.api.inject.SimpleInjector;
import play.core.j.DefaultJavaHandlerComponents;
import play.http.DefaultActionCreator;
import play.inject.DelegateInjector;
import play.libs.Scala;
import scala.compat.java8.OptionConverters;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Optional;

public class MyApplicationLoader implements ApplicationLoader {

    static class LoggerConfigurator {

        private final play.api.LoggerConfigurator delegate;

        public LoggerConfigurator(play.api.LoggerConfigurator delegate) {
            this.delegate = delegate;
        }

        public static Optional<LoggerConfigurator> fromClassLoader(ClassLoader classLoader) {
            return OptionConverters.toJava(LoggerConfigurator$.MODULE$.apply(classLoader)).map(LoggerConfigurator::new);
        }

        public void configure(Environment env) {
            delegate.configure(env.underlying());
        }
    }

    @Override
    public Application load(ApplicationLoader.Context context) {
        final ClassLoader classLoader = context.environment().classLoader();
        final Optional<LoggerConfigurator> opt = LoggerConfigurator.fromClassLoader(classLoader);
        opt.ifPresent(lc -> lc.configure(context.environment()));

        ApplicationComponent component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(context.underlying()))
                .build();

        return component.application();
    }

}

@Module
class ApplicationModule extends play.api.BuiltInComponentsFromContext {

    public ApplicationModule(play.api.ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public play.api.routing.Router router() {
        final HomeController homeController = new HomeController();
        return new router.Routes(httpErrorHandler(), homeController);
    }


    @Override
    public HttpRequestHandler httpRequestHandler() {
        return new JavaCompatibleHttpRequestHandler(
                router(),
                httpErrorHandler(),
                httpConfiguration(),
                new DefaultHttpFilters(httpFilters()),
                new DefaultJavaHandlerComponents(injector(), new DefaultActionCreator())
        );
    }

    @Provides
    public play.Application javaApplication() {
        final play.api.Application scalaApp = this.application();
        final DelegateInjector injector = new DelegateInjector(scalaApp.injector());
        return new DefaultApplication(scalaApp, injector);
    }

    @Override
    public play.api.inject.Injector injector() {
        // We need to add any Java actions and body parsers needed to the runtime injector
        return new SimpleInjector(super.injector(), Scala.asScala(new HashMap<Class<?>, Object>() {{
            put(play.mvc.BodyParser.Default.class, new play.mvc.BodyParser.Default(javaErrorHandler(), httpConfiguration()));
        }}));
    }

    play.http.HttpErrorHandler javaErrorHandler() {
        return new play.http.DefaultHttpErrorHandler(
                new play.Configuration(configuration().underlying()),
                new play.Environment(environment()),
                new OptionalSourceMapper(sourceMapper()),
                this::router
        );
    }
}

@Singleton
@Component(modules = { ApplicationModule.class })
interface ApplicationComponent {
    Application application();
}
