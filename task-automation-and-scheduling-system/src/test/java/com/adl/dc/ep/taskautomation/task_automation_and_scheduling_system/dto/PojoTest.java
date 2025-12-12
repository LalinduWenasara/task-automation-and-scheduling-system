package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.filters.FilterPackageInfo;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.affirm.Affirm;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class PojoTest {

    private static final int EXPECTED_CLASS_COUNT = 7;
    private static final String POJO_PACKAGE = "com/adl/dc/ep/taskautomation/task_automation_and_scheduling_system/dto";

    @Test
    void ensureExpectedPojoCount() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClasses(POJO_PACKAGE,
                new FilterPackageInfo());
        Affirm.affirmEquals("Classes added / removed?", EXPECTED_CLASS_COUNT, pojoClasses.size());
        Assertions.assertEquals(EXPECTED_CLASS_COUNT, pojoClasses.size());
    }

    @Test
    void testPojoStructureAndBehavior() {
        Validator validator = ValidatorBuilder.create()
                .with(new GetterMustExistRule())
                .with(new SetterMustExistRule())
                .with(new SetterTester())
                .with(new GetterTester())
                .build();
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClasses(POJO_PACKAGE);
        validator.validate(pojoClasses);
    }
}
