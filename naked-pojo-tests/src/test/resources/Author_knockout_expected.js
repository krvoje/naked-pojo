    function Author() {
        self = this;

        self.name=ko.observable("");
        self.surname=ko.observable("");
        self.book=ko.observable({});
        self.books=ko.observableArray([]);
        self.prolific=ko.observable(false);

        self.update = function(dto) {
            self.name(dto.name);
            self.surname(dto.surname);
            self.book(dto.book);
            self.books(dto.books);
            self.prolific(dto.prolific);
        }

        self.dto = function() {
            return ko.toJS(self);
        }
    }
