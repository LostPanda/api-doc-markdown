package top.kidhx.apidoc.checkinterface;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;

/**
 * @author HX
 * @date 2022/2/8
 */
@RestController
public class TestController {

    @PostMapping("/test/testApi")
    public Integer testApi(@Min(0) Long param){
        return 1;
    }
}
