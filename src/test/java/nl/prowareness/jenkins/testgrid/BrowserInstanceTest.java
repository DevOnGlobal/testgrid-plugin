/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Harm Pauw, Prowareness
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.prowareness.jenkins.testgrid;

import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by harm on 7-2-15.
 */
public class BrowserInstanceTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void doFillImageItems_shouldFillWithDockerImages() {
        TestgridBuildWrapper.DescriptorImpl descriptor = jenkins.getInstance().getDescriptorByType(TestgridBuildWrapper.DescriptorImpl.class);
        List<DockerImageSettings> images = new ArrayList<DockerImageSettings>();
        images.add(new DockerImageSettings("ff","ffimage"));
        images.add(new DockerImageSettings("chrome","chromeimage"));
        
        descriptor.setDockerImages(images);

        ListBoxModel model = new BrowserInstance("").getDescriptor().doFillImageItems();
        assertEquals(images.size(),model.size());
        for (int i = 0; i < model.size(); i++) {
            DockerImageSettings image = images.get(i);
            ListBoxModel.Option o = model.get(i);
            assertEquals(image.getName(), o.name);
            assertEquals(image.getImageName(),o.value);
        }
    }

    
}
