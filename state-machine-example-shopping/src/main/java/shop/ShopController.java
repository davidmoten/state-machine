package shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ShopController {

    @RequestMapping("/catalogs")
    public String catalog(Model model) {
        // model.addAttribute("catalogId", name);
        return "catalogs";
    }

}