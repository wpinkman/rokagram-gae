package com.rokagram.backend.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;

import com.google.appengine.api.datastore.Link;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.rokagram.backend.Alert;
import com.rokagram.backend.Constants;
import com.rokagram.backend.GaeClientLocation;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.UriBuilder;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.roku.Roku;
import com.rokagram.entities.UserEntity;
import com.rokagram.entities.UserLoveEntity;
import com.rokagram.entities.WhpEntity;

@SuppressWarnings("serial")
public class HomeServlet extends HttpServlet {
	public static final Logger log = Logger.getLogger(HomeServlet.class.getName());

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String rokuCode = req.getParameter(Constants.ROKUCODE);
		if (rokuCode != null) {
			if (org.apache.commons.lang.StringUtils.isEmpty(rokuCode)) {
				Alert.setErrorAlert(req, "Error", "The code was empty.");
				resp.sendRedirect("/");
			} else {
				Roku.handleDeviceCodeForm(req, resp, rokuCode.toUpperCase());
			}
		} else {
			Alert.setErrorAlert(req, "Error", "The code was empty.");
			resp.sendRedirect("/");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		List<String> segments = new ArrayList<String>();
		String servletPath = req.getServletPath();
		if (servletPath != null) {
			for (String segment : Splitter.on('/').omitEmptyStrings().trimResults().split(servletPath)) {
				segments.add(segment);
			}
		}
		if (segments.size() > 0) {
			handleSubHomeGet(req, resp, getServletContext(), segments, 0);
		} else {
			handleHomeGet(req, resp);
		}
	}

	private void handleSubHomeGet(HttpServletRequest req, HttpServletResponse resp, ServletContext servletContext,
			List<String> segments, int offset) throws IOException {

		String segment = segments.get(offset++);

		if (segment.equalsIgnoreCase(Constants.LOVE)) {
			handleLove(req, resp, servletContext, segments, offset);
		} else if (segment.equalsIgnoreCase(Constants.WHP)) {
			handleWhp(req, resp, segments, offset);
		}
	}

	private void handleWhp(HttpServletRequest req, HttpServletResponse resp, List<String> segments, int offset)
			throws IOException {
		int numSegments = segments.size();
		if (numSegments > offset) {
			String whptag = segments.get(offset);

			// List<WhpEntity> wpas = DAO.getWpaList();

			WhpEntity whp = DAO.ofy().load().type(WhpEntity.class).id(whptag).now();
			WhpEntity prev = DAO.ofy().load().type(WhpEntity.class).filter("announceDate >", whp.getAnnounceDate())
					.order("announceDate").first().now();
			WhpEntity next = DAO.ofy().load().type(WhpEntity.class).filter("announceDate <", whp.getAnnounceDate())
					.order("-announceDate").first().now();
			// WhpEntity whp = null;
			// WhpEntity prev = null;
			// WhpEntity next = null;
			//
			// int index = 0;
			// for (WhpEntity w : wpas) {
			//
			// if (w.getTag().equals(whptag)) {
			// whp = w;
			// }
			//
			// index++;
			// if (whp != null)
			// break;
			//
			// }
			//
			// if (index > 1) {
			// prev = wpas.get(index - 2);
			// }
			// if (index < wpas.size()) {
			// next = wpas.get(index);
			// }

			String servletPath = req.getServletPath();
			req.setAttribute("servletPath", servletPath);
			req.setAttribute("whp", whp);
			req.setAttribute("ogimage", whp.getAnnounceInsta().getStandard());
			req.setAttribute("description", whp.getAnnounceInsta().getCaption());

			req.setAttribute("next", next);
			req.setAttribute("prev", prev);

			// System.out.println("NEXT:" + next.getTag() + " " +
			// next.getAnnounceDateHuman());
			// System.out.println("PREV:" + prev.getTag() + " " +
			// prev.getAnnounceDateHuman());

			Utils.resolveAndRender("whp", req, resp);

		} else {
			List<WhpEntity> whps = DAO.getWpaList();
			WhpEntity mostRecent = whps.get(0);
			for (WhpEntity whp : whps) {
				if (whp.getAnnounceInsta() != null) {
					mostRecent = whp;
					break;
				}
			}
			Link ogimage = mostRecent.getAnnounceInsta().getStandard();
			req.setAttribute("ogimage", ogimage);
			req.setAttribute("numwhp", whps.size());
			req.setAttribute("whps", whps);
			Utils.resolveAndRender("whps", req, resp);
		}
	}

	private void handleLove(HttpServletRequest req, HttpServletResponse resp, ServletContext servletContext,
			List<String> segments, int offset) throws IOException {

		if (segments.size() > offset) {
			try {

				String fromUsername = null;
				String toUsername = null;
				String imageName = segments.get(offset);
				String[] parts = imageName.split(Constants.LOVE_SEPERATOR);

				if (parts.length == 2) {
					fromUsername = parts[0];
					toUsername = parts[1];
					if (toUsername.endsWith(Constants.LOVE_IMG_EXTENTION)) {
						toUsername = toUsername.substring(0, toUsername.indexOf(Constants.LOVE_IMG_EXTENTION));
					}
				}

				log.info("fromUsername: " + fromUsername + ", toUsername: " + toUsername);

				// User-Agent:
				// facebookexternalhit/1.1(+http://www.facebook.com/externalhit_uatext.php)
				// User-Agent: visionutils/0.2

				boolean facebook = false;
				@SuppressWarnings("unchecked")
				Enumeration<String> headerNames = req.getHeaderNames();
				while (headerNames.hasMoreElements()) {

					String headerName = headerNames.nextElement();
					@SuppressWarnings("unchecked")
					Enumeration<String> headers = req.getHeaders(headerName);

					while (headers.hasMoreElements()) {

						String headerValue = headers.nextElement();
						log.info(headerName + ": " + headerValue);
						if (headerName.contains("User-Agent")) {
							if (headerValue != null && headerValue.contains("facebook")
									|| headerValue.contains("visionutils")) {
								facebook = true;
							}
						}
					}
				}

				UserLoveEntity love = Instagram.makeLove(fromUsername, toUsername, servletContext, facebook);
				resp.setContentType("image/jpeg");
				resp.getOutputStream().write(love.getImageData());

			} catch (InterruptedException e) {
			} catch (ExecutionException e) {
			}
		} else {

			String q = req.getParameter("q");
			if (q != null) {
				List<UserEntity> users = Instagram.searchForUser(q);
				req.setAttribute("users", users);
				Utils.resolveAndRender("love-search-results", req, resp);
			} else {
				String fromParam = req.getParameter("from");
				String toParam = req.getParameter("to");

				String fromProfile = "/images/orientation-portrait-150.png";
				String toProfile = "/images/orientation-portrait-150.png";

				String fromUsername = null;
				String toUsername = null;

				StringBuilder imageNameBuilder = new StringBuilder();
				if (fromParam != null) {
					UserEntity from = Instagram.findUser(fromParam);
					if (from != null) {
						fromProfile = from.getProfile_picture();
						fromUsername = from.getUsername();
						imageNameBuilder.append(from.getUsername());
					}
				}

				imageNameBuilder.append(Constants.LOVE_SEPERATOR);

				if (toParam != null) {
					UserEntity to = Instagram.findUser(toParam);
					if (to != null) {
						toProfile = to.getProfile_picture();
						toUsername = to.getUsername();
						imageNameBuilder.append(to.getUsername());
					}
				}

				imageNameBuilder.append(Constants.LOVE_IMG_EXTENTION);

				UriBuilder b = UriBuilder.fromUri(Utils.isDevServer() ? Constants.LOCALHOST : "http://"
						+ req.getServerName());
				b.path(Constants.LOVE);
				b.path(imageNameBuilder.toString());

				UriBuilder ogurl = UriBuilder.fromUri(Utils.isDevServer() ? Constants.LOCALHOST : "http://"
						+ req.getServerName());
				ogurl.path(Constants.LOVE);
				if (fromParam != null) {
					ogurl.queryParam("from", fromParam);
				}
				if (toParam != null) {
					ogurl.queryParam("to", toParam);
				}

				String title = "Rokagram - Love";
				if (fromUsername != null && toUsername != null) {
					title = "Rokagram - " + fromUsername + " <3 " + toUsername;
				}

				String description = "Rokagram is the way to enjoy Instagram on your Roku-connected TV.";
				req.setAttribute("description", description);

				req.setAttribute("title", title);

				String imageSrc = b.toString();
				if (req.getParameter("admin") != null) {
					req.setAttribute("admin", true);
					req.setAttribute("fromUsername", fromParam);
					req.setAttribute("toUsername", toParam);
				}
				req.setAttribute("ogurl", ogurl.toString());
				req.setAttribute("imageSrc", imageSrc);
				req.setAttribute("fromProfile", fromProfile);
				req.setAttribute("toProfile", toProfile);

				Utils.resolveAndRender("love-bs", req, resp);
			}
		}
	}

	private void handleHomeGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String domain = req.getServerName();
		if (domain.contains("www") || Utils.isDevServer() || domain.contains("appspot")) {
			String redirect = "/";
			String code = req.getParameter(Constants.CODE);

			// String ownerAdd = "https://owner.roku.com/add/rokagram";
			String ownerAdd = "https://owner.roku.com/add/rokagramfreetrial";
			String buttonText = "Free Trial";

			GaeClientLocation location = new GaeClientLocation(req);
			if (!StringUtils.equalsIgnoreCase(location.getCountry(), "US")) {
				log.info("Returning Non US owner add URL for:" + location.toString());
				ownerAdd = "https://owner.roku.com/add/nonustrial";
			}
			req.setAttribute("owneradd", ownerAdd);
			req.setAttribute("buttonText", buttonText);

			// always start a session to avoid jessionid from appearing in URL
			HttpSession session = req.getSession();
			if (session.isNew()) {
				log.info("New session, domain:" + domain + ", location: " + location.getLocationShort());
			}

			String error = req.getParameter("error");
			String error_reason = req.getParameter("error_reason");
			String error_description = req.getParameter("error_description");

			if (Strings.isNullOrEmpty(code) && Strings.isNullOrEmpty(error)) {

				Utils.resolveAndRender("home", req, resp);

			} else {

				if (session.isNew()) {
					log.warning("Should not be new session");
					// Alert.setErrorAlert(req,
					// "Session timed out, please try again.",
					// error_description);
					// Utils.resolveAndRender("home", req, resp);
				}
				String state = (String) session.getAttribute(Constants.STATE);
				String stateParam = (String) req.getParameter(Constants.STATE);
				if (state != null && org.apache.commons.lang.StringUtils.equals(state, stateParam)) {
					if (code != null) {
						// do something

						UserEntity user = Instagram.fetchAccessToken(req, code);

						if (user != null) {
							req.setAttribute(Constants.USER, user);

							String rokuCode = (String) session.getAttribute(Constants.ROKUCODE);
							if (rokuCode != null) {
								boolean linked = Roku.createDeviceUserLinkage(req, rokuCode, user);
								if (linked) {
									redirect = "/";// "/love?from=getrokagram&to="
													// + user.getUsername();
								}
							}
						}

					} else {
						Utils.warn("code was null, error:" + error + ", error_reason:" + error_reason
								+ ", error_description:" + error_description);

						Alert.setErrorAlert(req, "Log in failed", error_description);

					}
					resp.sendRedirect(redirect);
				} else {
					if (stateParam == null) {
						log.warning("stateParam was null");
					}
					if (state == null) {
						log.warning("state was null");
					}
					if (state != null && stateParam != null) {
						log.warning("state:" + state + " != stateParam:" + stateParam);
					}

					resp.sendRedirect(redirect);
				}

			}
		} else {
			resp.sendRedirect("http://www." + domain);
		}
	}
}
