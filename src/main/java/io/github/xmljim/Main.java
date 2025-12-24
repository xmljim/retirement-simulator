package io.github.xmljim;

import io.github.xmljim.retirement.functions.Functions;

import java.time.LocalDate;
import java.time.Month;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static void main() {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.
        IO.println("Hello and welcome!");

        for (int i = 1; i <= 5; i++) {
            //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
            // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
            IO.println("i = " + i);
        }

        IO.println(Functions.INFLATION.apply(.03, 8));

        IO.println(Functions.ADJUSTED.apply(.03, 8));

        IO.println(Functions.IS_RETIRED.andThen(result -> {
            if (result) {
                IO.println("RETIRED");
            } else {
                IO.println("NOT RETIRED");
            }
            return result;
        }).apply(LocalDate.now(), LocalDate.now().minusDays(1)));

        IO.println(Functions.COLA.apply(184000.00, .02, 8));

        IO.println(Functions.INDIVIDUAL_CONTRIBUTION.apply(LocalDate.parse("2035-11-01"),
                LocalDate.parse("2034-01-01"), 0.14, 0.01, Month.JUNE));
    }
}
