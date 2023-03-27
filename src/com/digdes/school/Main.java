package com.digdes.school;

import com.digdes.school.*;


import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




public class Main {

    public static void main(String... args){
        JavaSchoolStarter starter = new JavaSchoolStarter();

        try {
            //Вставка строки в коллекцию
            List<Map<String,Object>> result1 = starter.execute("INSERT VALUES 'lastName' = 'Федоров' , 'id'=3, 'age'=40, 'active'=true");
            System.out.println(result1);
            List<Map<String,Object>> result5 = starter.execute("INSERT VALUES 'lastName' = 'Димас' , 'id'=4, 'age'=21, 'active'=true");
            System.out.println(result5);
            //Изменение значения которое выше записывали
            List<Map<String,Object>> result2 = starter.execute("UPDATE VALUES 'active'=false, 'cost'=10.1 where 'id'=3");
            System.out.println(result2);

            //Получение всех данных из коллекции (т.е. в данном примере вернется 1 запись)
            List<Map<String,Object>> result3 = starter.execute("SELECT");
            System.out.println(result3);

            List<Map<String,Object>> result4 = starter.execute("DELETE WHERE 'id' <= 3 OR 'cost'!= 10.1");
            System.out.println(result4);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
