package com.discushion.demo;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DemoPlanTests {
    @Test void requiresRegisteredNumericIdsAndExplicitNonemptyPlan() {
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of())));
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(new DemoProvisionMain.Entry("neighbor",0,1,null,null)))));
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(new DemoProvisionMain.Entry("neighbor",1,9007199254740992L,null,null)))));
    }
    @Test void separatesAccountsAndRejectsImplicitInstitutionDefaults() {
        var neighbor = new DemoProvisionMain.Entry("neighbor",1,1,null,null);
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(neighbor,neighbor))));
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(new DemoProvisionMain.Entry("institution",2,1,null,null)))));
        assertThrows(IllegalArgumentException.class, () -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(new DemoProvisionMain.Entry("unverified",2,1,3L,"2026-10-09T00:00:00Z")))));
    }
    @Test void acceptsExplicitCompletedAndExpiredInstitutionScenariosWithoutInventingValidity() {
        assertDoesNotThrow(() -> DemoProvisionMain.validate(new DemoProvisionMain.Plan(List.of(
            new DemoProvisionMain.Entry("unverified",1,1,null,null),
            new DemoProvisionMain.Entry("neighbor",2,1,null,null),
            new DemoProvisionMain.Entry("institution",3,1,7L,"2026-10-09T00:00:00Z"),
            new DemoProvisionMain.Entry("institution",4,1,7L,"2024-10-09T00:00:00Z")))));
    }
}
