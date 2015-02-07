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

import com.strangeberry.jmdns.tools.Browser;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created by harm on 5-2-15.
 */
public class BrowserInstance extends AbstractDescribableImpl<BrowserInstance> {
    private String image;

    @DataBoundConstructor
    public BrowserInstance(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BrowserInstance> {
        
        public ListBoxModel doFillImageItems() {
            ListBoxModel items = new ListBoxModel();

            TestgridBuildWrapper.DescriptorImpl descriptor = Jenkins.getInstance().getDescriptorByType(TestgridBuildWrapper.DescriptorImpl.class);
            
            for (DockerImageSettings image : descriptor.getDockerImages()) {
                items.add(image.getName(),image.getImageName());
            }
            
            descriptor.getDockerImages();
            return items;
        }
        
        public String getDisplayName() {
            return "";
        }
    }
}
