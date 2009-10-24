/* Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.spock

import grails.util.GrailsNameUtils
import grails.util.GrailsWebUtil
import groovy.xml.StreamingMarkupBuilder

import org.codehaus.groovy.grails.commons.ApplicationAttributes

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse

import org.codehaus.groovy.grails.support.MockApplicationContext

import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService

import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import grails.test.*
import spock.lang.*

/**
 * Support class for writing unit tests for controllers. Its main job
 * is to mock the various properties and methods that Grails injects
 * into controllers. By default it determines what controller to mock
 * based on the name of the test, but this can be overridden by one
 * of the constructors.
 * 
 * @author Graeme Rocher
 * @author Peter Ledbrook
 */
class ControllerSpecification extends UnitSpecification {

    @Shared controllerClass
    final controller
    
    private webRequest
    
    def setupSpeck() {
      def m = getClass().name =~ /^([\w\.]*?[A-Z]\w*?Controller)\w+/
      if (m) {
          controllerClass = Thread.currentThread().contextClassLoader.loadClass(m[0][1])
      } else {
          throw new RuntimeException("Cannot find matching class for this test.")
      }
    }
    

    def getMockRequest() { controller.request }
    def getMockResponse() { controller.response }
    def getMockSession() { controller.session }

    def getForwardArgs() { controller.forwardArgs }
    def getRedirectArgs() { controller.redirectArgs }
    def getRenderArgs() { controller.renderArgs }
    def getChainArgs() { controller.chainArgs }
    
    def getMockParams() { controller.params }
    def getMockFlash() { controller.flash }

    def setup() {
        super.setup()
        
        mockController(controllerClass)
  
        controller = controllerClass.newInstance()

        MockApplicationContext ctx = new MockApplicationContext()
        ctx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())
        mockRequest.servletContext.setAttribute(ApplicationAttributes.APPLICATION_CONTEXT, ctx)
        
        webRequest = new GrailsWebRequest(
            mockRequest,
            mockResponse,
            mockRequest.servletContext
       )
       
        mockRequest.setAttribute(GrailsApplicationAttributes.WEB_REQUEST, webRequest)
        RequestContextHolder.setRequestAttributes(webRequest)
        
        webRequest.controllerName = GrailsNameUtils.getLogicalPropertyName(controllerClass.name, "Controller")
    }

    protected void reset() {
        mockRequest.clearAttributes()
        mockRequest.removeAllParameters()
        mockResponse.committed = false
        mockSession.clearAttributes()
        mockSession.setNew(true)

        forwardArgs.clear()
        redirectArgs.clear()
        renderArgs.clear()
        mockParams.clear()
        mockFlash.clear()
    }
    
    /**
     * Mocks a command object class, providing a "validate()" method
     * and access to the "errors" property.
     */
    protected mockCommandObject(Class clazz) {
        registerMetaClass(clazz)
        MockUtils.mockCommandObject(clazz, errorsMap)
    }

    protected void setXmlRequestContent(content) {
        setXmlRequestContent("UTF-8", content)
    }

    protected void setXmlRequestContent(String encoding, content) {
        mockRequest.contentType = "application/xml; charset=$encoding"
        
        if (content instanceof Closure) {
          def xml = new StreamingMarkupBuilder(encoding: encoding).bind(content)
          def out = new ByteArrayOutputStream()
          out << xml

          mockRequest.contentType = "application/xml; charset=$encoding"
          mockRequest.content = out.toByteArray()
        } else {
          mockRequest.content = content.getBytes(encoding)
        }
    }
}