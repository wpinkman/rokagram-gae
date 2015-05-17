package com.rokagram.backend;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.google.appengine.api.datastore.GeoPt;
import com.google.common.base.Strings;

public class GaeClientLocation {
	private static final String X_APP_ENGINE_COUNTRY = "X-AppEngine-Country";
	private static final String X_APP_ENGINE_REGION = "X-AppEngine-Region";
	private static final String X_APP_ENGINE_CITY = "X-AppEngine-City";
	private static final String X_APP_ENGINE_CITY_LAT_LONG = "X-AppEngine-CityLatLong";

	private String country;
	private String region;
	private String city;

	private String cityLatLong;

	public GaeClientLocation() {
	}

	public GaeClientLocation(HttpServletRequest req) {
		this.country = req.getHeader(X_APP_ENGINE_COUNTRY);
		this.region = req.getHeader(X_APP_ENGINE_REGION);
		this.city = req.getHeader(X_APP_ENGINE_CITY);
		this.setCityLatLong(req.getHeader(X_APP_ENGINE_CITY_LAT_LONG));

		if (Utils.isDevServer()) {
			setDevServerDummyValues();
		}

		// handle some special cases
		if (this.getCityLatLong() != null && this.getCityLatLong().contains("0.0")) {
			if (StringUtils.equals(this.country, "BM")) {
				this.setCityLatLong("32.3333,64.7500");
				this.city = "Bermuda";
			}
			if (StringUtils.equals(this.country, "PR")) {
				this.setCityLatLong("18.464792,-66.104175");
				this.city = "San Juan";
			}
			if (StringUtils.equals(this.country, "TT")) {
				this.setCityLatLong("10.671067,-61.521206");
				this.city = "Port of Spain";
			}
			if (StringUtils.equals(this.country, "SR")) {
				this.setCityLatLong("5.8667,55.1667");
				this.city = "Paramaribo";
			}

			// Bahamas
			if (StringUtils.equals(this.country, "BS")) {
				this.setCityLatLong("25.0600,77.3450");
				this.city = "Nassau";
			}
			// Anguilla
			if (StringUtils.equals(this.country, "AI")) {
				this.setCityLatLong("18.2272,63.0490");
				this.city = "The Valley";
			}
			// Saint Kitts And Nevis
			if (StringUtils.equals(this.country, "KN")) {
				this.setCityLatLong("17.3000,62.7333");
				this.city = "Saint Kitts And Nevis";
			}
		}
	}

	public GaeClientLocation(String city, String region, String country) {
		this.city = city;
		this.region = region;
		this.country = country;
	}

	public String getCountryName() {
		String ret = this.country;
		if (this.country != null) {
			try {
				Locale l = new Locale("", this.country);
				ret = l.getDisplayCountry();
			} catch (Exception ex) {

			}
		}
		return ret;
	}

	public String getCityCapitalized() {
		String ret = null;
		if (this.city != null) {
			ret = WordUtils.capitalize(this.city);
		}
		return ret;
	}

	public String getCountryCapitalized() {
		String ret = null;
		if (this.country != null) {
			ret = this.country.toUpperCase();
		}
		return ret;
	}

	public String getLocation() {
		return getLocationString(true);
	}

	public String getLocationShort() {
		return getLocationString(false);
	}

	private String getLocationString(boolean full) {
		String ret = "unknown";

		StringBuilder sb = new StringBuilder();

		if (this.city != null && !this.city.equalsIgnoreCase("?")) {
			sb.append(this.getCityCapitalized());
			sb.append(", ");
		}
		if (this.region != null && !this.region.equalsIgnoreCase("?")) {
			sb.append(this.getRegionCapitalized());
			sb.append(", ");
		}

		if (this.country != null && !this.country.equalsIgnoreCase("?")) {
			String cuntName = this.getCountryName();
			if (full) {
				if (cuntName == null) {
					sb.append(this.country.toUpperCase());
				} else {
					sb.append(cuntName);
				}

			} else {
				if (!this.country.equalsIgnoreCase("US")) {
					sb.append(this.country.toUpperCase());
				}
			}
		}

		ret = sb.toString();
		if (ret.endsWith(", ") && ret.length() > 3) {
			ret = ret.substring(0, ret.length() - 2);
		}

		return ret;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setCityLatLong(String cityLatLong) {
		this.cityLatLong = cityLatLong;
	}

	public String getCityLatLong() {
		return cityLatLong;
	}

	public GeoPt getCityLatLongAsGeoPt() {
		GeoPt ret = null;

		if (this.cityLatLong != null) {
			String[] parts = this.cityLatLong.split(",");
			if (parts.length == 2) {
				try {
					ret = new GeoPt(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()));
				} catch (NumberFormatException nfe) {
				}
			}
		}

		return ret;
	}

	public void setDevServerDummyValues() {
		if (Strings.isNullOrEmpty(city) || Strings.isNullOrEmpty(this.cityLatLong)) {

			if (Utils.isDevServer()) {
				int random = (int) (Math.random() * (3 + 1));
				// random = 6;
				System.out.println("Random location: " + random);
				switch (random) {
				case 0:
					this.city = "los angeles";
					this.cityLatLong = "34.052235,-118.24368";
					this.region = "ca";
					this.country = "US";

					break;
				case 1:
					this.city = "glasgow";
					this.cityLatLong = "55.86424,-4.251806";
					this.region = "sct";
					this.country = "GB";

					break;
				case 2:
					this.city = "?";
					this.cityLatLong = "0.0,0.0";
					this.region = "?";
					this.country = "PR";

					break;
				case 3:
					this.city = "miami";
					this.cityLatLong = "25.788969,-80.22644";
					this.region = "fl";
					this.country = "US";

					break;
				case 4:
					this.city = "?";
					this.cityLatLong = "0.0,0.0";
					this.region = "?";
					this.country = "SR";

					break;
				case 5:
					this.city = "?";
					this.cityLatLong = "0.0,0.0";
					this.region = "?";
					this.country = "AI";

					break;
				case 6:
					this.city = "?";
					this.cityLatLong = "0.0,0.0";
					this.region = "?";
					this.country = "KN";

					break;
				default:
					this.city = "mountain view";
					this.cityLatLong = "37.386051,-122.083851";
					this.region = "ca";
					this.country = "US";
					break;
				}
			}
		}
	}

	public String getRegionCapitalized() {
		String ret = null;
		if (this.region != null) {
			ret = this.region.toUpperCase();
		}
		return ret;
	}

	@Override
	public String toString() {
		String ret = "unknown";

		StringBuilder sb = new StringBuilder();

		if (this.city != null) {
			sb.append(this.getCityCapitalized());
			if (this.region != null) {
				sb.append(",");
				sb.append(this.getRegionCapitalized());
				if (this.country != null) {
					if (!this.country.equals("US")) {
						sb.append(",");
						sb.append(this.country.toUpperCase());
					}
				}
			}
			ret = sb.toString();
		}

		return ret;
	}

}
