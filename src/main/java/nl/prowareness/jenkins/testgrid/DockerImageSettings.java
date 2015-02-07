/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Harm Pauw <h.pauw@prowareness.nl>
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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import nl.prowareness.jenkins.testgrid.utils.DockerImageNameValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

@SuppressWarnings({"WeakerAccess", "UnusedDeclaration"})
public class DockerImageSettings extends AbstractDescribableImpl<DockerImageSettings> {

    private final String name;
    private final String imageName;

    @DataBoundConstructor
    public DockerImageSettings(String name, String imageName) {
        this.name = name;
        this.imageName = imageName;
    }

    public String getName() {
        return name;
    }

    public String getImageName() {
        return imageName;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DockerImageSettings> {
        public String getDisplayName() {
            return "";
        }

        public FormValidation doCheckImageName(@QueryParameter String value) {
            if (DockerImageNameValidator.validate(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("Invalid format.");
            }
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return value.length() > 0 ? FormValidation.ok() : FormValidation.error("Cannot be empty");
        }
    }

}
