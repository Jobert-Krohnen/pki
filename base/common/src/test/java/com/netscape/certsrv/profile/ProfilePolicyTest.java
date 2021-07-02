package com.netscape.certsrv.profile;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.netscape.certsrv.util.JSONSerializer;

public class ProfilePolicyTest {

    private static ProfilePolicy before = new ProfilePolicy();

    @Before
    public void setUpBefore() {
        before.setConstraint(new PolicyConstraint());
        before.setDef(new PolicyDefault());
        before.setId("foo");
    }

    @Test
    public void testJSON() throws Exception {
        // Act
        String json = before.toJSON();
        System.out.println("JSON (before): " + json);

        ProfilePolicy afterJSON = JSONSerializer.fromJSON(json, ProfilePolicy.class);
        System.out.println("JSON (after): " + afterJSON.toJSON());

        // Assert
        Assert.assertEquals(before, afterJSON);
    }

}
