package com.afp.iris.sr.wm.presentation.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.afp.iptc.g2.libg2api.Keyword;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaTopic {

	String name;
	URI uri;
	List<Keyword> enKeywords;
	List<Keyword> frKeywords;

	public static List<MediaTopic> getSamples() {

		List<MediaTopic> mediaTopics = new ArrayList<>();

		MediaTopic education = new MediaTopic();
		education.setName("Education");
		education.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/05000000"));
		education.setEnKeywords(Arrays.asList(new Keyword("education")));
		education.setFrKeywords(Arrays.asList(new Keyword("éducation")));

		MediaTopic weather = new MediaTopic();
		weather.setName("Meteo");
		weather.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/17000000"));
		weather.setEnKeywords(Arrays.asList(new Keyword("weather")));
		weather.setFrKeywords(Arrays.asList(new Keyword("météo")));

		MediaTopic politic = new MediaTopic();
		politic.setName("Politique");
		politic.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/11000000"));
		politic.setEnKeywords(Arrays.asList(new Keyword("politics")));
		politic.setFrKeywords(Arrays.asList(new Keyword("politique")));

		MediaTopic sante = new MediaTopic();
		sante.setName("Santé");
		sante.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/07000000"));
		sante.setEnKeywords(Arrays.asList(new Keyword("health")));
		sante.setFrKeywords(Arrays.asList(new Keyword("santé")));

		MediaTopic science = new MediaTopic();
		science.setName("Science et technologie");
		science.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/13000000"));
		science.setEnKeywords(Arrays.asList(new Keyword("science")));
		science.setFrKeywords(Arrays.asList(new Keyword("sciences")));

		MediaTopic labour = new MediaTopic();
		labour.setName("Social");
		labour.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/09000000"));
		labour.setEnKeywords(Arrays.asList(new Keyword("labour")));
		labour.setFrKeywords(Arrays.asList(new Keyword("social")));

		MediaTopic social = new MediaTopic();
		social.setName("Société");
		social.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/14000000"));
		social.setEnKeywords(Arrays.asList(new Keyword("social")));
		social.setFrKeywords(Arrays.asList(new Keyword("société")));

		MediaTopic lifestyle = new MediaTopic();
		lifestyle.setName("Vie quotidienne et loisirs");
		lifestyle.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/10000000"));
		lifestyle.setEnKeywords(Arrays.asList(new Keyword("lifestyle")));
		lifestyle.setFrKeywords(Arrays.asList(new Keyword("loisirs")));

		MediaTopic environment = new MediaTopic();
		environment.setName("Environnement");
		environment.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/06000000"));
		environment.setEnKeywords(Arrays.asList(new Keyword("environment")));
		environment.setFrKeywords(Arrays.asList(new Keyword("environnement")));

		MediaTopic religion = new MediaTopic();
		religion.setName("Religion et croyance");
		religion.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/12000000"));
		religion.setEnKeywords(Arrays.asList(new Keyword("religion")));
		religion.setFrKeywords(Arrays.asList(new Keyword("religion")));

		MediaTopic accidents = new MediaTopic();
		accidents.setName("Désastres et accidents");
		accidents.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/03000000"));

		MediaTopic finance = new MediaTopic();
		finance.setName("Economie et finances");
		finance.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/04000000"));

		MediaTopic animal = new MediaTopic();
		animal.setName("Gens animaux insolite");
		animal.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/08000000"));

		MediaTopic sport = new MediaTopic();
		sport.setName("Sport");
		sport.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/15000000"));

		MediaTopic conflict = new MediaTopic();
		conflict.setName("Conflits, guerres et paix");
		conflict.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/16000000"));

		MediaTopic art = new MediaTopic();
		art.setName("Arts, culture et divertissement");
		art.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/01000000"));

		MediaTopic justice = new MediaTopic();
		justice.setName("Criminalité, droit et justice");
		justice.setUri(URI.create("http://cv.iptc.org/newscodes/mediatopic/02000000"));

		mediaTopics.add(education);
		mediaTopics.add(weather);
		mediaTopics.add(politic);
		mediaTopics.add(science);
		mediaTopics.add(labour);
		mediaTopics.add(social);
		mediaTopics.add(sante);
		mediaTopics.add(lifestyle);
		mediaTopics.add(environment);
		mediaTopics.add(religion);
		mediaTopics.add(accidents);
		mediaTopics.add(finance);
		mediaTopics.add(animal);
		mediaTopics.add(sport);
		mediaTopics.add(conflict);
		mediaTopics.add(art);
		mediaTopics.add(justice);

		return mediaTopics;

	}
}
