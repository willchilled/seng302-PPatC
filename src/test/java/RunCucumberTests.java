import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

/**
 * Created by kre39 on 7/08/17.
 */

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/test/java/features")
public class RunCucumberTests {
}
