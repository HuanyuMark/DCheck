import org.example.dcheck.api.DuplicateChecking;
import org.example.dcheck.spi.DuplicateCheckingProvider;
import org.junit.jupiter.api.Test;

/**
 * Date 2025/02/28
 *
 * @author 三石而立Sunsy
 */
public class DcheckAggregateTest {

    @Test
    public void test() {
        DuplicateChecking checking = DuplicateCheckingProvider.getInstance().getChecking();
        checking.init();
//        checking.check()
    }

    @Test
    public void testArrayCopy() {

    }
}
