package macrobase.runtime;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import macrobase.conf.MacroBaseConf;
import macrobase.runtime.resources.AnalyzeResource;
import macrobase.runtime.resources.RowSetResource;
import macrobase.runtime.resources.SchemaResource;
import macrobase.runtime.standalone.MacroBaseBatchCommand;
import macrobase.runtime.standalone.MacroBaseContextualBatchCommand;
import macrobase.runtime.standalone.MacroBaseStreamingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacroBaseServer extends Application<MacroBaseConf> {
    private static final Logger log = LoggerFactory.getLogger(MacroBaseServer.class);

    public static void main(String[] args) throws Exception {
        new MacroBaseServer().run(args);
    }

    @Override
    public String getName() {
        return "macrobase";
    }

    @Override
    public void initialize(Bootstrap<MacroBaseConf> bootstrap) {
        bootstrap.addCommand(new MacroBaseBatchCommand());
        bootstrap.addCommand(new MacroBaseStreamingCommand());
        bootstrap.addCommand(new MacroBaseContextualBatchCommand());
        bootstrap.addBundle(new AssetsBundle("/frontend", "/", "console.html"));
    }

    @Override
    public void run(MacroBaseConf configuration,
                    Environment environment) throws Exception {
        configuration.loadSystemProperties();
        environment.jersey().register(new SchemaResource(configuration));
        environment.jersey().register(new RowSetResource(configuration));
        environment.jersey().register(new AnalyzeResource(configuration));

        environment.healthChecks().register("basic", new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return null;
            }
        });

        environment.jersey().setUrlPattern("/api/*");
    }
}