package com.rokagram.backend.velocity;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import com.googlecode.objectify.Key;
import com.rokagram.backend.dao.DAO;
import com.rokagram.entities.TemplateEntity;

public class HrdResourceLoader extends ResourceLoader {

	public static final Logger log = Logger.getLogger(HrdResourceLoader.class.getName());

	@Override
	public long getLastModified(Resource arg0) {
		return arg0.getLastModified();
	}

	@Override
	public InputStream getResourceStream(String template) throws ResourceNotFoundException {

		InputStream is = null;
		try {

			String tString = null;
			{
				TemplateEntity te = DAO.ofy().load().key(Key.create(TemplateEntity.class, template)).now();
				if (te != null) {

					if (te.isCurrent()) {
						tString = te.getTemplate();
					}
				}

				if (tString == null) {
					te = new TemplateEntity();
					te.setName(template);
					String fullFileName = VelocityService.TEMPLATES_DIR + "/" + template;
					is = new FileInputStream(fullFileName);
					StringWriter writer = new StringWriter();

					IOUtils.copy(is, writer);
					tString = writer.toString();

					te.setTemplate(tString);

					DAO.ofy().save().entity(te);
				}
				return new ByteArrayInputStream(tString.getBytes());
			}
		} catch (FileNotFoundException e) {
			if (!template.contains("VM_global_library")) {
				log.warning("FileNotFoundException template " + template + " message:" + e.getMessage());
			}
		} catch (IOException e) {
			log.severe(e.getMessage());
		}

		throw new ResourceNotFoundException(template);
	}

	@Override
	public void init(ExtendedProperties configuration) {

	}

	@Override
	public boolean isSourceModified(Resource arg0) {
		return false;
	}

}
