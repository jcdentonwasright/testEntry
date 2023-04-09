package com.digdes.school;

import com.digdes.school.*;


import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Main {

    public static void main(String... args){
        JavaSchoolStarter starter = new JavaSchoolStarter();

        try {
            //Вставка строки в коллекцию
            List<Map<String,Object>> result1 = starter.execute("INSERT VALUES 'lastName' = 'Федоров' , 'id'=3, 'age'=40, 'active'=true");
            System.out.println(result1);

            List<Map<String,Object>> result5 = starter.execute("INSERT VALUES 'lastName' = 'Димас' , 'id'=4, 'cost' = null ,'age'=21, 'active'=true");
            System.out.println(result5);

            //Изменение значения которое выше записывали
            List<Map<String,Object>> result2 = starter.execute("UPDATE VALUES 'active'=false, 'cost'=10.1 where 'id'=3");
            System.out.println(result2);

            //Получение всех данных из коллекции (т.е. в данном примере вернется 2 записи)
            List<Map<String,Object>> result3 = starter.execute("SELECT");
            System.out.println(result3);

            //Удаление строк коллекции в которых lastName имеет букву д
             List<Map<String,Object>> result4 = starter.execute("DELETE WHERE 'lastname' like '%д%' OR ( 'id'=3 OR 'id'=4) AND 'age' = 40");
            System.out.println(result4);

            //Получение всех оставшихся данных из коллекции
            List<Map<String,Object>> result6 = starter.execute("SELECT");
            System.out.println(result3);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
